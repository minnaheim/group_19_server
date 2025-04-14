package ch.uzh.ifi.hase.soprafs25.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;

@Repository("moviePoolRepository")
public interface MoviePoolRepository extends JpaRepository<MoviePool, Long> {
    
    MoviePool findByGroup_GroupId(Long groupId);
} 