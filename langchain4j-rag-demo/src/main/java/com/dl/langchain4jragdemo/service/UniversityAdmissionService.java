package com.dl.langchain4jragdemo.service;

import com.dl.langchain4jragdemo.mapper.UniversityScoreCacheMapper;
import com.dl.langchain4jragdemo.pojo.MajorScore;
import com.dl.langchain4jragdemo.pojo.University;
import com.dl.langchain4jragdemo.pojo.UniversityScoreCache;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * =====================================================================================
 * 高校录取分数查询服务
 * -------------------------------------------------------------------------------------
 * 核心职责：
 *   1. 对接「掌上高考」公开 REST API，获取高校基本信息与专业录取分数线
 *   2. 采用「MySQL + 内存」二级缓存策略，减少重复外部调用，提升查询性能
 *   3. 对输入参数（省份名、学校名、专业关键词等）做模糊匹配与兼容处理，降低调用方使用门槛
 *
 * 数据流向（主链路 getMajorScores）：
 *   用户参数 → 省份名解析为ID → 学校名搜索定位学校 → 读MySQL缓存(命中则返回)
 *        → 未命中则分页拉取API → 序列化写入MySQL缓存 → 按科类/专业关键词过滤 → 返回
 * =====================================================================================
 */
@Slf4j
@Service
public class UniversityAdmissionService {

    /**
     * 掌上高考业务接口域名。
     * 所有业务接口均通过 /web/api/ 网关路由，真正的业务 path 放在 uri 查询参数中。
     */
    private static final String API_HOST = "api.zjzw.cn";

    /**
     * 省份配置静态资源地址。
     * 该接口返回所有省份的 ID 与中文名映射，用于将「广东/广东省」等用户输入转换为 API 所需的数字 ID。
     */
    private static final String PROVINCE_CONFIG_URL = "https://static-data.gaokao.cn/www/2.0/config/81004.json";

    /**
     * 浏览器 User-Agent 请求头。
     * 部分接口做了简单反爬校验，缺少 UA 或 UA 异常会返回空数据，这里使用主流桌面浏览器标识。
     */
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    /**
     * 单页数据条数，与上游接口约定一致（上游接口默认/上限均为 20）。
     */
    private static final int PAGE_SIZE = 20;

    /**
     * 最大翻页数，作为死循环熔断保护。
     * 即使上游接口异常始终返回满页数据，也只拉取 50 页 = 1000 条专业后强制停止，
     * 避免耗尽连接池资源或出现 OOM。
     */
    private static final int MAX_PAGE = 50;

    /**
     * Spring RestClient 建造器，用于构造带默认请求头的 HTTP 客户端。
     * 每次 client() 调用都会基于它 build 新实例，建造器本身不可变因此线程安全。
     */
    @Resource
    private RestClient.Builder restClientBuilder;

    /**
     * MyBatis Mapper：操作 university_score_cache 缓存表。
     * 缓存粒度为 (school_id, province_id, year) 三元组，value 为完整 MajorScore 列表的 JSON。
     */
    @Resource
    private UniversityScoreCacheMapper universityScoreCacheMapper;

    /**
     * Jackson JSON 处理器：用于缓存 JSON 的序列化（写缓存）与反序列化（读缓存）。
     * Spring Boot 会自动装配配置好的 ObjectMapper Bean。
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 省份名称 → 省份数字ID 的内存缓存（JVM 级别单例）。
     *
     * 使用 volatile 修饰原因（双重检查锁 DCL 的标准写法）：
     *   1. 保证可见性：一个线程完成 HashMap 初始化后，其他线程能立刻看到引用不为 null
     *   2. 禁止指令重排：防止「将 provinceIdMap 引用指向一块已分配但尚未完成构造的内存」这种半初始化逸出
     */
    private volatile Map<String, String> provinceIdMap;

    /**
     * 根据学校名称搜索学校列表（调用掌上高考学校搜索接口）。
     *
     * <p>接口路由约定：所有业务接口走 /web/api/ 网关，真实业务路径通过 uri 查询参数传递，
     * 这是第三方数据平台常见的「统一网关 + 内部路由」架构。
     *
     * <p>结果排序：精确匹配（name 完全相等）的学校排在最前面，
     * 保证用户搜「清华大学」时第一条一定是目标学校，而不是模糊匹配的附属机构。
     *
     * @param name 学校名称关键词，支持模糊搜索
     * @return 匹配的学校列表；若接口异常则返回空列表（降级策略，不抛异常）
     */
    public List<University> searchUniversity(String name) {
        JsonNode root;
        try {
            // 调用掌上高考学校搜索接口：apidata/api/gk/school/lists
            root = client().get()
                    .uri(uri -> uri.scheme("https").host(API_HOST).path("/web/api/")
                            .queryParam("keyword", name)   // 搜索关键词
                            .queryParam("page", 1)        // 固定取第1页
                            .queryParam("size", 20)       // 最多返回20所学校
                            .queryParam("uri", "apidata/api/gk/school/lists") // 业务路由
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            // 接口异常降级：打错误日志并返回空列表，不向上层抛异常中断业务
            log.error("搜索学校失败 name={}", name, e);
            return new ArrayList<>();
        }

        List<University> list = new ArrayList<>();
        // 接口返回结构: { data: { item: [ {...}, {...} ] } }
        JsonNode items = root.path("data").path("item");
        if (items.isArray()) {
            for (JsonNode item : items) {
                // 将 JSON 字段逐一映射到 University POJO
                University u = new University();
                u.setSchoolId(item.path("school_id").asLong());          // 学校唯一ID（主键）
                u.setName(item.path("name").asText(null));               // 学校全称
                u.setProvinceName(item.path("province_name").asText(null)); // 所在省份
                u.setCityName(item.path("city_name").asText(null));      // 所在城市
                u.setLevelName(item.path("level_name").asText(null));    // 办学层次（本科/专科）
                u.setNatureName(item.path("nature_name").asText(null));  // 办学性质（公办/民办）
                u.setTypeName(item.path("type_name").asText(null));      // 院校类型（综合/理工/师范...）
                list.add(u);
            }
        }
        // 精确匹配优先排序：完全相等的排在前面（0 < 1），模糊匹配的在后面
        list.sort(Comparator.comparingInt(u -> name != null && name.equals(u.getName()) ? 0 : 1));
        return list;
    }

    /**
     * 查询某学校某年某省份的专业录取分数 —— 本服务的主入口方法。
     *
     * <p>执行流程（四步流水线）：
     * <ol>
     *   <li>解析省份ID：将省份中文名模糊匹配为 API 所需的数字 ID</li>
     *   <li>定位学校：通过学校名搜索，取匹配度最高的第 1 所学校</li>
     *   <li>获取分数数据：优先读 MySQL 缓存，未命中则拉取上游 API 并回填缓存</li>
     *   <li>结果过滤：按科类（物理/历史等）和专业关键词二次过滤</li>
     * </ol>
     *
     * <p>缓存粒度说明：缓存存的是「某校某省某年的全部专业数据」，而不是按专业粒度拆分，
     * 原因是：一次缓存可以服务多种过滤条件的后续查询，缓存命中率显著更高。
     *
     * @param schoolName    学校名称（支持模糊匹配，但只会取搜索结果第1个）
     * @param year          录取年份，默认 2024
     * @param provinceName  省份名称（支持「广东」「广东省」等模糊输入）
     * @param typeName      科类关键词过滤（如「物理类」「历史类」「理科」），null/空则不过滤
     * @param majorKeyword  专业名称关键词过滤（如「计算机」），null/空则不过滤
     * @return 符合条件的专业录取分列表；任一步骤失败均返回空列表
     */
    public List<MajorScore> getMajorScores(String schoolName, Integer year, String provinceName, String typeName, String majorKeyword) {
        // 年份为空时默认查询最新的 2024 年数据（上游接口也有默认值，此处显式设置便于调试）
        if (year == null) {
            year = 2024;
        }
        // ── 第 1 步：省份名 → 省份数字 ID ──
        String provinceId = resolveProvinceId(provinceName);
        if (provinceId == null) {
            log.warn("无法解析省份 provinceName={}", provinceName);
            return new ArrayList<>();
        }
        // ── 第 2 步：学校名 → 学校对象（取最匹配的第 1 所）──
        List<University> schools = searchUniversity(schoolName);
        if (schools.isEmpty()) {
            log.warn("未找到学校 schoolName={}", schoolName);
            return new ArrayList<>();
        }
        University school = schools.get(0);

        // ── 第 3 步：缓存优先获取全量专业分数 ──
        List<MajorScore> all = getFromCacheOrFetch(school.getSchoolId(), provinceId, year);

        // ── 第 4 步：在内存中按科类 / 专业关键词过滤 ──
        return all.stream()
                // 科类过滤：typeName 为空就全放行，否则要求专业的 typeName 包含关键词
                .filter(s -> isBlank(typeName) || (s.getTypeName() != null && s.getTypeName().contains(typeName)))
                // 专业名称过滤：majorKeyword 为空就全放行，否则模糊包含匹配
                .filter(s -> isBlank(majorKeyword) || (s.getMajorName() != null && s.getMajorName().contains(majorKeyword)))
                .collect(Collectors.toList());
    }

    /**
     * 缓存优先策略（Cache-Aside 旁路缓存模式）读取专业分数。
     *
     * <p>缓存模式说明：
     * <pre>
     *           读请求
     *             │
     *             ▼
     *        ┌──────────┐
     *        │ 查MySQL  │──命中──► JSON反序列化为 List<MajorScore> ──► 返回
     *        └────┬─────┘
     *             │未命中 / 反序列化失败
     *             ▼
     *        ┌──────────┐
     *        │ 拉上游API│
     *        └────┬─────┘
     *             ▼
     *        ┌──────────┐
     *        │ 写回缓存 │（序列化为JSON存入 score_data TEXT字段）
     *        └────┬─────┘
     *             ▼
     *           返回
     * </pre>
     *
     * <p>容错降级设计（3处）：
     *   1. 缓存反序列化失败（例如 JSON 格式不兼容旧版本）→ 降级为直接拉 API，不抛异常
     *   2. 缓存写入失败（例如 DB 连接超时）→ 只打 warn 日志，下次查询还会重试
     *   3. API 返回空列表 → 不写缓存（避免缓存穿透），直接返回空
     *
     * @param schoolId   学校ID（主键）
     * @param provinceId 省份ID（数字字符串）
     * @param year       录取年份
     * @return 专业录取分数列表
     */
    private List<MajorScore> getFromCacheOrFetch(Long schoolId, String provinceId, Integer year) {
        // ── 尝试命中 MySQL 缓存 ──
        UniversityScoreCache cache = universityScoreCacheMapper.get(schoolId, provinceId, year);
        if (cache != null && cache.getScoreData() != null) {
            try {
                // 将缓存中的 JSON 字符串反序列化为 List<MajorScore>
                // TypeReference 用于保留泛型信息（List<MajorScore> 的擦除问题）
                return objectMapper.readValue(cache.getScoreData(), new TypeReference<List<MajorScore>>() {
                });
            } catch (Exception e) {
                // 反序列化失败（典型原因：POJO 字段变更、缓存JSON格式不一致）
                // 打 warn 日志后走降级逻辑：当缓存未命中处理，不影响业务可用性
                log.warn("缓存数据解析失败，重新拉取 schoolId={} year={}", schoolId, year, e);
            }
        }
        // ── 缓存未命中或解析失败：调用上游接口拉取 ──
        List<MajorScore> scores = fetchMajorScores(schoolId, provinceId, year);
        // ── 拉取结果非空时写入缓存（空结果不写，避免缓存穿透）──
        if (!scores.isEmpty()) {
            try {
                UniversityScoreCache c = new UniversityScoreCache();
                c.setSchoolId(schoolId);
                c.setProvinceId(provinceId);
                c.setYear(year);
                // 将完整列表序列化为 JSON 字符串存入 TEXT 字段
                // 这是「粗粒度缓存」的核心：一次写入可服务多种过滤查询
                c.setScoreData(objectMapper.writeValueAsString(scores));
                universityScoreCacheMapper.insert(c);
            } catch (Exception e) {
                // 缓存写入失败不影响主流程：只打日志，下次查询还会重新尝试
                log.warn("缓存写入失败 schoolId={} year={}", schoolId, year, e);
            }
        }
        return scores;
    }

    /**
     * 分页拉取某学校某省某年的全部专业录取分数（上游接口限制单页最多 20 条）。
     *
     * <p>分页终止的双重保障机制：
     * <ol>
     *   <li><b>业务终止条件</b>：本页返回条数 {@code < PAGE_SIZE} → 说明已是最后一页，正常结束</li>
     *   <li><b>安全熔断条件</b>：循环次数达到 {@code MAX_PAGE} → 即使接口异常一直返回满页，也强制停止，防止死循环</li>
     * </ol>
     *
     * <p>关于 {@code final int currentPage = page}：
     * Java Lambda 表达式只能捕获 final 或 effectively final 的变量，而 for 循环中 page 是不断自增的，
     * 因此必须在循环体内拷贝一个 final 副本才能在 uri lambda 中引用当前页码。
     *
     * @param schoolId   学校ID
     * @param provinceId 省份ID
     * @param year       录取年份
     * @return 全部专业分数列表（聚合所有页）
     */
    private List<MajorScore> fetchMajorScores(Long schoolId, String provinceId, Integer year) {
        List<MajorScore> result = new ArrayList<>();
        RestClient client = client();
        // 从第 1 页开始翻，最多翻 MAX_PAGE 页防止死循环
        for (int page = 1; page <= MAX_PAGE; page++) {
            // lambda 需要 final 变量：将循环变量 page 拷贝为 currentPage
            final int currentPage = page;
            JsonNode root;
            try {
                // 调用专业录取分接口：apidata/api/gk/score/special
                root = client.get()
                        .uri(uri -> uri.scheme("https").host(API_HOST).path("/web/api/")
                                .queryParam("local_province_id", provinceId) // 考生所在省份ID
                                .queryParam("page", currentPage)              // 当前页码
                                .queryParam("school_id", schoolId)            // 学校ID
                                .queryParam("size", PAGE_SIZE)                // 每页条数
                                .queryParam("uri", "apidata/api/gk/score/special") // 业务路由
                                .queryParam("year", year)                     // 录取年份
                                .build())
                        .retrieve()
                        .body(JsonNode.class);
            } catch (Exception e) {
                // 单页请求失败：打 error 日志后 break，返回已拉取的部分数据（部分失败降级）
                log.error("拉取录取分数失败 schoolId={} provinceId={} year={} page={}", schoolId, provinceId, year, page, e);
                break;
            }
            JsonNode items = root.path("data").path("item");
            if (!items.isArray()) {
                // 返回结构不符合预期（例如登录失效、限流返回错误 JSON）→ 终止翻页
                break;
            }
            // 逐条映射接口字段 → MajorScore POJO
            for (JsonNode item : items) {
                MajorScore m = new MajorScore();
                m.setSchoolName(item.path("name").asText(null));                 // 学校名称
                m.setMajorName(item.path("spname").asText(null));                // 专业名称（specialty name → spname）
                m.setBatchName(item.path("local_batch_name").asText(null));      // 录取批次（本科批/专科批/提前批...）
                m.setTypeName(item.path("local_type_name").asText(null));        // 科类（物理类/历史类/理科/文科...）
                m.setMinScore(item.path("min").asText(null));                    // 最低录取分数
                m.setMinRank(item.path("min_section").asText(null));             // 最低录取位次（省排名）
                m.setProscore(item.path("proscore").asText(null));               // 专业分（部分艺术类专业有）
                m.setYear(year);                                                 // 录取年份（接口未回传，由入参回填）
                result.add(m);
            }
            // 本页条数不足 PAGE_SIZE → 已是最后一页，无需继续翻
            if (items.size() < PAGE_SIZE) {
                break;
            }
        }
        return result;
    }

    /**
     * 省份名称 → 省份数字ID 的模糊匹配解析。
     *
     * <p>双向包含匹配策略（容错用户输入不规范）：
     * <ul>
     *   <li>用户输入「广东」→ 匹配配置中的「广东省」（前者包含于后者）</li>
     *   <li>用户输入「内蒙古自治区」→ 匹配配置中的「内蒙古」（后者包含于前者）</li>
     * </ul>
     * 注意：遍历顺序取决于 HashMap，若存在多匹配歧义则返回第一个遇到的，
     * 在实际场景中「省份名」几乎不会有歧义，因此这样设计足够。
     *
     * @param provinceName 省份名称（支持简称/全称/带省/市/自治区后缀）
     * @return 省份数字ID（如「44」代表广东）；无法匹配时返回 null
     */
    private String resolveProvinceId(String provinceName) {
        if (isBlank(provinceName)) {
            return null;
        }
        // 获取省份映射内存缓存（首次调用会触发远程加载）
        Map<String, String> map = getProvinceIdMap();
        // 遍历所有省份，做双向包含模糊匹配
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (provinceName.contains(e.getKey()) || e.getKey().contains(provinceName)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * 获取「省份名→ID」映射的内存缓存单例。
     *
     * <p>使用 <b>双重检查锁（Double-Checked Locking, DCL）</b> 模式保证：
     * <ol>
     *   <li><b>延迟初始化</b>：只有第一次调用时才会触发远程接口加载</li>
     *   <li><b>线程安全</b>：并发时只初始化一次，不会出现两个线程同时拉接口</li>
     *   <li><b>高性能</b>：初始化完成后的 99% 调用都不需要加锁，只做一次 volatile 读</li>
     * </ol>
     *
     * <p>DCL 三步分解：
     * <pre>
     * 1. 第1次检查（无锁，读volatile）：map != null → 直接返回，避免99%场景加锁
     * 2. 加 synchronized 锁：保证只有一个线程进入初始化块
     * 3. 第2次检查（锁内，再读volatile）：防止 A、B 两线程都通过第1次检查后，B又重复初始化
     * </pre>
     *
     * @return 不可变的省份名→ID Map（加载失败时返回空 Map，不会返回 null）
     */
    private Map<String, String> getProvinceIdMap() {
        // 将 volatile 字段拷贝到局部变量，减少 volatile 读次数（DCL 标准优化写法）
        Map<String, String> map = provinceIdMap;
        if (map != null) {
            return map; // 第1次检查：已初始化 → 无锁快路径返回
        }
        synchronized (this) { // 加类实例级别内置锁
            map = provinceIdMap;
            if (map == null) { // 第2次检查：锁内二次确认，防止重复初始化
                map = loadProvinceIdMap(); // 真正执行远程加载
                provinceIdMap = map;       // volatile 写：保证其他线程可见
            }
        }
        return map;
    }

    /**
     * 从掌上高考静态配置接口加载「省份名→ID」映射表。
     *
     * <p>接口返回结构示例：
     * <pre>
     * {
     *   "data": {
     *     "11": { "provinceName": "北京市", ... },
     *     "44": { "provinceName": "广东省", ... },
     *     "15": { "provinceName": "内蒙古", ... }
     *   }
     * }
     * </pre>
     *
     * <p>加载策略：使用 {@code data.fields()} 遍历 JSON 对象的所有键值对，
     * 反向建立 {@code Map<provinceName, provinceId>} 索引，便于后续按名称模糊查找。
     * 加载失败（网络异常/接口返回异常）时打 error 日志并返回空 Map，
     * 上层会因匹配不到省份ID返回空结果，不会导致服务崩溃。
     *
     * @return 省份中文名 → 数字ID 的 HashMap；加载失败返回空 Map
     */
    private Map<String, String> loadProvinceIdMap() {
        Map<String, String> map = new HashMap<>();
        try {
            // 拉取静态配置 JSON（该 URL 部署在 CDN，稳定性高）
            JsonNode root = client().get().uri(PROVINCE_CONFIG_URL).retrieve().body(JsonNode.class);
            JsonNode data = root.path("data");
            // data 是一个以「省份ID」为 key 的对象，遍历所有字段建立反向索引
            data.fields().forEachRemaining(e ->
                    // e.getKey()   = 省份数字ID（如 "44"）
                    // e.getValue() = 省份信息对象，取 provinceName 字段作为 Map 的 key
                    map.put(e.getValue().path("provinceName").asText(), e.getKey()));
        } catch (Exception e) {
            // 加载失败降级：记录 error 日志并返回空 Map（不会返回 null 避免上层 NPE）
            log.error("加载省份id配置失败", e);
        }
        return map;
    }

    /**
     * 构造带默认 User-Agent 请求头的 RestClient 实例。
     *
     * <p>线程安全说明：
     * RestClient.Builder 的 defaultHeader() / build() 均为不可变操作，
     * 每次调用会返回新的 Builder/RestClient 实例，因此多线程共用 restClientBuilder 无并发问题。
     *
     * @return 配置好 UA 的 RestClient 新实例
     */
    private RestClient client() {
        return restClientBuilder.defaultHeader("User-Agent", UA).build();
    }

    /**
     * 判断字符串是否为 null 或空串/全空白字符。
     * 相当于 StringUtils.isBlank() 的精简实现，避免引入额外 commons-lang3 依赖。
     *
     * @param s 待检查字符串
     * @return true = 空/空白/为null；false = 非空有效字符串
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
