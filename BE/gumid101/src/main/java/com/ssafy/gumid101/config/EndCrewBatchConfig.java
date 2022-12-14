package com.ssafy.gumid101.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ssafy.gumid101.crew.RunRecordRepository;
import com.ssafy.gumid101.crew.UserCrewJoinRepository;
import com.ssafy.gumid101.entity.CrewEntity;
import com.ssafy.gumid101.entity.FCMEntity;
import com.ssafy.gumid101.entity.RunRecordEntity;
import com.ssafy.gumid101.entity.UserCrewJoinEntity;
import com.ssafy.gumid101.entity.UserEntity;
import com.ssafy.gumid101.firebase.FirebaseMessageStoreUtil;
import com.ssafy.gumid101.firebase.FirebaseMessageUtil;
import com.ssafy.gumid101.user.UserRepository;

import io.jsonwebtoken.lang.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableBatchProcessing
@Configuration
@RequiredArgsConstructor
public class EndCrewBatchConfig {

	private final JobBuilderFactory jobBuilderFactory;
	private final StepBuilderFactory stepBuilderFactory;
	private final EntityManagerFactory entityManagerFactory;
	private final int CHUNK_SIZE = 10;
	private final RunRecordRepository runRepo;
	private final UserCrewJoinRepository userCrewJoinRepo;
	private final FirebaseMessageStoreUtil fcmStore;
	private final UserRepository userRepo;
	
	
	@Bean("crewEndJob")
	@Qualifier("crewEndJob")
	public Job crewEndJop() {

		Job exampleJob = jobBuilderFactory.get("crewEndJop").start(pointCalculatingStep()).build();
		return exampleJob;
	}

	@Bean
	@JobScope // ?????? ???????????? ??????
	public Step pointCalculatingStep() {
		log.debug("????????? ?????? JOB??? ???????????????.");
		return stepBuilderFactory.get("pointCalculatingStep").<CrewEntity, CrewEntity>chunk(CHUNK_SIZE)
				.reader(endCrewReader(null))
				.processor(endCrewPointCalculateProcessor())
				.writer(calculatedPintWriter())
				.build();

	}

	@Bean
	@StepScope
	public JpaPagingItemReader<CrewEntity> endCrewReader(
			@Value("#{jobParameters[requestDate]}") final String requestDate) {
		log.debug("????????? ?????? JOB-Reader ????????? ???????????????.");
		Map<String, Object> parameterValues = new HashMap<>();
		parameterValues.put("crewCheckYn", "N");
		parameterValues.put("nowTime", LocalDateTime.now());
		return new JpaPagingItemReaderBuilder<CrewEntity>().pageSize(CHUNK_SIZE).parameterValues(parameterValues)
				.queryString(
						"SELECT c FROM CrewEntity c where c.crewCheckYn = :crewCheckYn AND c.crewDateEnd < :nowTime ORDER BY c.crewSeq")
				.entityManagerFactory(entityManagerFactory).name("crewPointCalculationReader").build();
		// ?????? ????????? ????????? ?????? ???????????? ?????? ????????? ?????????.
	}

	@Bean
	@StepScope
	public ItemProcessor<CrewEntity, CrewEntity> endCrewPointCalculateProcessor() {

		return new ItemProcessor<CrewEntity, CrewEntity>() {

			@Override
			public CrewEntity process(CrewEntity crewEntity) throws Exception {
				log.debug("????????? ?????? JOB-Process ????????? ???????????????.");
				List<UserCrewJoinEntity> userCrewList = userCrewJoinRepo.findAllByCrewEntity(crewEntity);

				// ???????????? 0????????? ????????? ?????? 0??? ????????? ????????? ?????? ??????.
				if (crewEntity.getCrewCost() == null || crewEntity.getCrewCost() <= 0 || userCrewList.size() == 0) {
					crewEntity.setCrewCheckYn("Y");
					return crewEntity;
				}

				Map<Long, Long> userSucceedDays = new HashMap<>();

				// ?????? ????????? ????????? ????????????.
				List<RunRecordEntity> crewRunRecords = runRepo
						.findByCrewEntityAndRunRecordCompleteYNOrderByRunRecordStartTime(crewEntity, "Y");

				// ?????? ????????? ??????
				Long totalPoint = (long) userCrewList.size() * crewEntity.getCrewCost();
				long totalSucceedDay = 0;
				// ????????? ??? ????????? 1????????? ???
				LocalDateTime weeksEnd = crewEntity.getCrewDateStart().plusDays(6).plusHours(23).plusMinutes(59)
						.plusSeconds(59);
				int idx = 0;
				while (idx < crewRunRecords.size() && !weeksEnd.isAfter(crewEntity.getCrewDateEnd())) {
					Map<Long, Long> weekSucceedDays = new HashMap<>();
					while (idx < crewRunRecords.size()
							&& !crewRunRecords.get(idx).getRunRecordStartTime().isAfter(weeksEnd)) {
						long userSeq = crewRunRecords.get(idx).getUserEntity().getUserSeq();
						if (!weekSucceedDays.containsKey(userSeq)) {
							weekSucceedDays.put(userSeq, 0L);
						}
						if (weekSucceedDays.get(userSeq) < (long) crewEntity.getCrewGoalDays()) {
							weekSucceedDays.put(userSeq, weekSucceedDays.get(userSeq) + 1);
							if (!userSucceedDays.containsKey(userSeq)) {
								userSucceedDays.put(userSeq, 0L);
							}
							userSucceedDays.put(userSeq, userSucceedDays.get(userSeq) + 1);
							totalSucceedDay++;
						}
						idx++;
					}
					weeksEnd = weeksEnd.plusDays(7);
				}

				// ????????? ??? ??? ????????? ????????? ??? ??? ????????? ??????????????? ?????????.
				if (totalSucceedDay <= 0) {
					crewEntity.setCrewCheckYn("Y");
					return crewEntity;
				}

				UserEntity user = null;
				for (int i = 0; i < userCrewList.size(); i++) {
					if (userSucceedDays.containsKey(userCrewList.get(i).getUserEntity().getUserSeq())) {
						// ????????? ?????? ????????? ??????????????? Integer???????????? ??? ?????????. (????????? ?????? ??????????????? long?????????.)
						user = userCrewList.get(i).getUserEntity();

						int point = (int) (totalPoint * userSucceedDays.get(userCrewList.get(i).getUserEntity().getUserSeq())
										/ totalSucceedDay);

						userRepo.updatePointAsBulk(user.getUserSeq(),point);
						
						if (Strings.hasLength(user.getFcmToken())) {
							// Message message = FcmMessage.ofMessage(user.getFcmToken(), "RunWithMe [?????????
							// ??????]", String.format("%s ????????? ???????????? ???????????? ?????????????????????.\n????????? ?????????: %d",
							// userCrewList.get(i).getCrewEntity().getCrewName(),point));
							fcmStore.storeFcmMessage(user.getFcmToken(), "RunWithMe [????????? ??????]",
									String.format("%s ????????? ???????????? ???????????? ?????????????????????.\n????????? ?????????: %d",
											userCrewList.get(i).getCrewEntity().getCrewName(), point),
									user.getUserSeq());
						}

					}
				}
				crewEntity.setCrewCheckYn("Y");
				return crewEntity;
			}

		};
	}

	@Bean
	@StepScope // ????????? ?????? ??????
	public JpaItemWriter<CrewEntity> calculatedPintWriter() {
		log.debug("????????? ?????? JOB-Writer ????????? ???????????????.");

		return new JpaItemWriterBuilder<CrewEntity>().entityManagerFactory(entityManagerFactory).build();
	}

	

}



