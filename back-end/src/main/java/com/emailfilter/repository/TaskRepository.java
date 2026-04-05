package com.emailfilter.repository;

import com.emailfilter.entity.Priority;
import com.emailfilter.entity.Task;
import com.emailfilter.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(Priority priority);

    List<Task> findByStatusAndPriority(TaskStatus status, Priority priority);

    List<Task> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Task t ORDER BY " +
           "CASE t.priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END, " +
           "t.createdAt DESC")
    List<Task> findAllOrderByPriorityAndDate();

    long countByStatus(TaskStatus status);

    long countByPriority(Priority priority);

    boolean existsByTaskNameAndEmail_Id(String taskName, Long emailId);
}
