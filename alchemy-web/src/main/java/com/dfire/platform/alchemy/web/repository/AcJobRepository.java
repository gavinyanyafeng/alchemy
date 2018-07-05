package com.dfire.platform.alchemy.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dfire.platform.alchemy.web.domain.AcJob;

@Repository
public interface AcJobRepository extends JpaRepository<AcJob, Long> {

    @Modifying
    @Query(value = "update ac_job set status=?2 where id=?1 ", nativeQuery = true)
    @Transactional
    void updateJobStatus(Long id, int status);

}