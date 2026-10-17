package com.dl.langchain4jragdemo;

import com.dl.langchain4jragdemo.mapper.ReservationMapper;
import com.dl.langchain4jragdemo.pojo.Reservation;
import com.dl.langchain4jragdemo.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MockTest01 {

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void testInsert() {

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setName("张三");
        reservation.setGender("男");
        reservation.setPhone("13800000000");
        reservation.setCommunicationTime(LocalDateTime.now());
        reservation.setProvince("北京");
        reservation.setEstimatedScore(90);

        when(reservationMapper.findByPhone("13800000000"))
                .thenReturn(reservation);

        Reservation byPhone = reservationService.findByPhone("13800000000");
        System.out.println(byPhone);

        verify(reservationMapper)
                .findByPhone("13800000000");

    }
}
