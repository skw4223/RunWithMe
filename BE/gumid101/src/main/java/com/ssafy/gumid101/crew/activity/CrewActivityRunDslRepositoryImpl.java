package com.ssafy.gumid101.crew.activity;

import java.beans.Expression;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.ListPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.gumid101.competition.CompetitionResultStatus;
import com.ssafy.gumid101.dto.CrewTotalRecordDto;
import com.ssafy.gumid101.dto.RankingParamsDto;
import com.ssafy.gumid101.dto.RecordParamsDto;
import com.ssafy.gumid101.dto.RunRecordDto;
import com.ssafy.gumid101.entity.QCrewEntity;
import com.ssafy.gumid101.entity.QCrewTotalRecordEntity;
import com.ssafy.gumid101.entity.QImageFileEntity;
import com.ssafy.gumid101.entity.QRunRecordEntity;
import com.ssafy.gumid101.entity.QUserCrewJoinEntity;
import com.ssafy.gumid101.entity.QUserEntity;
import com.ssafy.gumid101.entity.RunRecordEntity;
import com.ssafy.gumid101.entity.UserCrewJoinEntity;
import com.ssafy.gumid101.res.RankingDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class CrewActivityRunDslRepositoryImpl implements CrewActivityRunDslRepository {

	@Autowired
	 private JdbcTemplate jdbcTemplate;
	
	@Autowired
	JPAQueryFactory factory;

	@Override
	public List<RunRecordEntity> getRunRecords(RecordParamsDto condition) {

		QRunRecordEntity qRunRecordEntity = QRunRecordEntity.runRecordEntity;
		QUserEntity qUser = qRunRecordEntity.userEntity;
		QCrewEntity qCrew = qRunRecordEntity.crewEntity;
		QImageFileEntity qImage = qRunRecordEntity.imageFile;

		BooleanBuilder builder = new BooleanBuilder();

		if (condition.getCrewSeq() != null && condition.getCrewSeq() > 0) {
			builder.and(qCrew.crewSeq.eq(condition.getCrewSeq()));
		}
		if (condition.getUserSeq() != null && condition.getUserSeq() > 0) {
			builder.and(qUser.userSeq.eq(condition.getUserSeq()));
		}
		if (condition.getMonth() != null && condition.getMonth() != 0) {
			builder.and(qRunRecordEntity.runRecordRegTime.month().eq(condition.getMonth()));
		}
		if (condition.getYear() != null && condition.getYear() != 0) {
			builder.and(qRunRecordEntity.runRecordRegTime.year().eq(condition.getYear()));
		}
		if (condition.getMaxRunRecordSeq() != null && condition.getMaxRunRecordSeq() != 0) {
			builder.and(qRunRecordEntity.runRecordSeq.lt(condition.getMaxRunRecordSeq()));
		}

		JPAQuery<RunRecordEntity> jpaQuery = factory.selectFrom(qRunRecordEntity).innerJoin(qUser).innerJoin(qCrew).fetchJoin()
				.innerJoin(qImage).where(builder)
				.orderBy(qRunRecordEntity.runRecordSeq.desc(), qRunRecordEntity.runRecordRegTime.desc());

		if (condition.getSize() != null && condition.getSize() >= 0) {
			jpaQuery.limit(condition.getSize());
		}

		// maxRunRecordSeq
		List<RunRecordEntity> results = jpaQuery.fetch();

		return results;
	}

	/**
	 * ????????? ?????? ?????????
	 */
	@Override
	public List<RunRecordDto> selectByCrewAndUserSeqWithOffsetSize(RecordParamsDto recordParamsDto) {

		QRunRecordEntity runRecord = new QRunRecordEntity("runRecord");

		Long userSeq = recordParamsDto.getUserSeq();
		Long cruewSeq = recordParamsDto.getCrewSeq();
		Integer size = recordParamsDto.getSize();

		BooleanBuilder builder = new BooleanBuilder();
		if (size == null || size == 0) {
			size = Integer.MAX_VALUE;
		}

		Long maxUserSeq = (long) Optional.ofNullable(recordParamsDto.getMaxRunRecordSeq()).orElseGet(() -> {
			return 0;
		});
		if (maxUserSeq != null && maxUserSeq != 0) {
			builder.and(runRecord.userEntity.userSeq.lt(maxUserSeq));
		}

		Integer year = recordParamsDto.getYear();
		if (year != null && year != 0) {
			builder.and(runRecord.runRecordRegTime.year().eq(year));
		}
		Integer month = recordParamsDto.getMonth();
		if (month != null && month != 0) {
			builder.and(runRecord.runRecordRegTime.month().eq(month));
		}

		List<RunRecordEntity> myRecordInCrews = factory.selectFrom(runRecord)
				.where(runRecord.userEntity.userSeq.eq(userSeq).and(runRecord.crewEntity.crewSeq.eq(cruewSeq)))
				.where(builder).orderBy(runRecord.runRecordEndTime.desc(), runRecord.runRecordSeq.desc()).limit(size)
				.fetch();

		List<RunRecordDto> myRecordListInCrew = myRecordInCrews.stream().map((entitiy) -> {
			return RunRecordDto.of(entitiy);
		}).collect(Collectors.toList());

		return myRecordListInCrew;
	}

	// ???????????? ??????????????? ?????? ?????? 3?????? ????????? 6??? ????????? ??????

	/**
	 * ??????????????? ??????/.
	 */

	@Override
	public List<RankingDto> selectAllCrewRanking(RankingParamsDto paramDto) {

		QCrewTotalRecordEntity runTotalRecord = new QCrewTotalRecordEntity("runTotalRecord"); // ??????-?????? ????????????
		Long crewSeq = paramDto.getCrewSeq();
		String type = paramDto.getType();

		NumberPath<Integer> targetField = runTotalRecord.totalDistance;
		if ("distance".equals(type)) {

		} else if ("time".equals(type)) {
			targetField = runTotalRecord.totalTime;
		} else if ("success".equals(type)) {
			return selectAllCrewSuccessRanking(paramDto);
		} else {
			log.warn("??????????????? ???????????? invoke in : ", CrewActivityRunDslRepositoryImpl.class.getName());
		}

		List<RankingDto> rankigs = factory.from(runTotalRecord)
				.select(Projections.fields(RankingDto.class, //
						runTotalRecord.userEntity.userSeq.as("userSeq"), //
						runTotalRecord.userEntity.nickName.as("userName"), //
						runTotalRecord.userEntity.competitionResult.as("competitionResult"), //
						targetField.as("rankingValue"), //
						runTotalRecord.userEntity.imageFile.imgSeq.as("imgSeq")))
				.where(runTotalRecord.crewEntity.crewSeq.eq(crewSeq))
				.orderBy(targetField.desc(), runTotalRecord.userEntity.userSeq.asc()).fetch();

		for (int i = 0; i < rankigs.size(); i++) {
			rankigs.get(i).setRankingIndex(i + 1);
		}

		return rankigs;
	}

	private List<RankingDto> selectAllCrewSuccessRanking(RankingParamsDto paramDto) {
		
		Long crewSeq = paramDto.getCrewSeq();
		String sql  = "with sub1 as  (select tu.user_seq as user_seq ,tu.img_seq as imgSeq,tu.user_nickname as user_nickname, tu.competition_result as competition_result from t_crew_user as tcu "+ 
				"inner join t_user as tu "+
				"on tu.user_seq = tcu.user_seq "+
				"where tcu.crew_seq = ? ) "+
				"select row_number() over (order by count(trr.run_record_seq) desc,user_seq asc) as rankingIndex,imgSeq, sub1.user_seq,sub1.user_nickname,count(trr.run_record_seq) as rankingValue, sub1.competition_result as competitionResult from sub1  "+
				"left outer join t_run_record trr "+
				"on trr.user_seq = sub1.user_seq and trr.crew_seq = ? and trr.run_record_complete_yn = 'Y' "+
				"group by sub1.user_seq "+
				"order by rankingIndex ";
		
		List<RankingDto> rankingList = jdbcTemplate.query(
				sql,
				new RowMapper<RankingDto>() {
					@Override
					public RankingDto mapRow(ResultSet rs, int rowNum) throws SQLException {
						RankingDto dto = new RankingDto();
						dto.setImgSeq(	rs.getLong("imgSeq"));
						dto.setRankingIndex((int)rs.getLong("rankingIndex"));
						dto.setRankingValue((int)rs.getLong("rankingValue"));
						dto.setCompetitionResult(CompetitionResultStatus.valueOf(rs.getString("competitionResult")));
						dto.setUserName(rs.getString("user_nickname"));
						dto.setUserSeq(rs.getLong("user_seq"));
						return dto;
					}
				}, crewSeq,crewSeq);

		

		return rankingList;
	}

	/**
	 * ???????????? ?????? ?????? ??????
	 */
	@Override
	public CrewTotalRecordDto selectByCrewSeqAnduserSeq(Long crewSeq, Long userSeq) {
		QCrewTotalRecordEntity runToalRecord = new QCrewTotalRecordEntity("runTotalRecord");
		// factory.from(runToalRecord)

		CrewTotalRecordDto myTotalRecordInCrew = null;
		try {

			myTotalRecordInCrew = factory.from(runToalRecord).select(Projections.fields(CrewTotalRecordDto.class,
					runToalRecord.totalCalorie.sum().as("totalCalorie"),
					runToalRecord.totalDistance.sum().as("totalDistance"), runToalRecord.totalTime.as("totalTime"),
					runToalRecord.totalLongestDistance.max().as("totalLongestDistance"),
					runToalRecord.totalLongestTime.max().as("totalLongestTime")))
					.where(runToalRecord.crewEntity.crewSeq.eq(crewSeq)
							.and(runToalRecord.userEntity.userSeq.eq(crewSeq)))
					.fetchOne();
		} catch (Exception e) {
			log.info("??????-?????? ?????? ?????? ??? ??????, ???????????? ????????? ?????? ?????? ??????,");
		}
		if(myTotalRecordInCrew == null) {
			myTotalRecordInCrew = CrewTotalRecordDto.defaultCrewTotalRecordDto();
		}
		return myTotalRecordInCrew;
	}

}