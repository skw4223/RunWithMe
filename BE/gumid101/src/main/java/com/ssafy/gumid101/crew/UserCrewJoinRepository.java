package com.ssafy.gumid101.crew;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ssafy.gumid101.entity.CrewEntity;
import com.ssafy.gumid101.entity.UserCrewJoinEntity;
import com.ssafy.gumid101.entity.UserEntity;

@Repository
public interface UserCrewJoinRepository extends JpaRepository<UserCrewJoinEntity, Long>{

	@Modifying
	@Query(value = "DELETE FROM UserCrewJoinEntity ucj WHERE ucj.crewEntity = :crewSeq")
	int deleteAllBycrewSeq(@Param("crewSeq")CrewEntity crewSeq);

	@Modifying(flushAutomatically = true,clearAutomatically = true)
	//포인트는 비관적락을 걸고,벌크 연산을 통해 전체 환급시킨다.
	@Query(value="UPDATE UserEntity u SET u.point = u.point + COALESCE(:cost,0) "+
	"WHERE u IN (SELECT ucj.userEntity FROM UserCrewJoinEntity ucj INNER JOIN ucj.crewEntity c WHERE c = :crew) ")
	int pointRefunds(@Param("crew") CrewEntity crew,int cost);

	@Modifying(flushAutomatically = true,clearAutomatically = true) //쿼리전 플러시, 쿼리후 플러시 
	@Query("DELETE FROM UserCrewJoinEntity ucj WHERE ucj.userEntity = :user and ucj.crewEntity = :crew")
	int deleteByUserAndCrew(UserEntity user, CrewEntity crew);

	Optional<UserCrewJoinEntity> findByUserEntity_UserSeqAndCrewEntity_CrewSeq(Long userSeq, Long crewSeq);
	Optional<UserCrewJoinEntity> findByUserEntityAndCrewEntity(UserEntity userEntity, CrewEntity crewEntity);

	List<UserCrewJoinEntity> findAllByCrewEntity(CrewEntity crew);

	@Query("SELECT count(*) FROM UserCrewJoinEntity ucj where ucj.crewEntity.crewSeq = :crewSeq")
	int findCountCrewUser(Long crewSeq);

	@Query("SELECT c FROM UserCrewJoinEntity ucj JOIN ucj.crewEntity c WHERE ucj.userEntity.userSeq = :userSeqParam AND c.crewDateEnd < now()")
	List<CrewEntity> selectByUserSeqAndCrewDateEndBeforeNow(@Param("userSeqParam") Long userSeq);
	
	
}
