package com.healthtrack.repository;

import com.healthtrack.entity.Bill;
import com.healthtrack.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findByIdLocked(@Param("id") Long id);

    Optional<Bill> findByIdempotencyKey(String idempotencyKey);

    List<Bill> findByCreatedAtAfter(LocalDateTime since);

    List<Bill> findByPatientId(Long patientId);

    List<Bill> findByHospitalIdAndPaymentStatus(Long hospitalId, PaymentStatus paymentStatus);

    long countByHospitalIdAndPaymentStatus(Long hospitalId, PaymentStatus paymentStatus);

    @Query("select b from Bill b where b.hospital.id = :hospitalId and b.createdAt between :start and :end")
    List<Bill> findByHospitalIdAndCreatedAtBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(b) from Bill b where b.hospital.id = :hospitalId and b.createdAt between :start and :end")
    long countByHospitalIdAndCreatedAtBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("start") java.time.OffsetDateTime start,
            @Param("end") java.time.OffsetDateTime end);

    @Query(value = """
        select extract(month from b.created_at) as month, extract(year from b.created_at) as year, sum(b.net_payable)
        from bill b where b.hospital_id = :hospitalId and b.created_at >= :since
        group by extract(year from b.created_at), extract(month from b.created_at)
        order by extract(year from b.created_at), extract(month from b.created_at)
    """, nativeQuery = true)
    List<Object[]> sumNetPayableByHospitalAndMonth(@Param("hospitalId") Long hospitalId, @Param("since") LocalDateTime since);
}