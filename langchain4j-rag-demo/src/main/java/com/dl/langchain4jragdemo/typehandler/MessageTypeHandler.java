package com.dl.langchain4jragdemo.typehandler;

import com.dl.langchain4jragdemo.enums.MessageType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MessageType 枚举与 varchar("user"/"ai") 之间的转换
 */
public class MessageTypeHandler extends BaseTypeHandler<MessageType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MessageType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public MessageType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return MessageType.getEnumByValue(rs.getString(columnName));
    }

    @Override
    public MessageType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return MessageType.getEnumByValue(rs.getString(columnIndex));
    }

    @Override
    public MessageType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return MessageType.getEnumByValue(cs.getString(columnIndex));
    }
}
