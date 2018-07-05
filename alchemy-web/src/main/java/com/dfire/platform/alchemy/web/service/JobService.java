package com.dfire.platform.alchemy.web.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dfire.platform.alchemy.web.rest.vm.JobVM;
import com.dfire.platform.alchemy.web.service.dto.JobDTO;

import javax.validation.Valid;

/**
 * 对job的增删改查
 *
 * @author congbai
 * @date 2018/6/8
 */
public interface JobService {

    void save(JobVM jobVM);

    Page<JobDTO> list(Pageable pageable);

    void updateStatus(Long id, int status);

    void delete(Long id);

    JobDTO findById(Long id);

}