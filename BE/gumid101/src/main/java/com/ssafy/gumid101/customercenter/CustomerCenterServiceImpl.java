package com.ssafy.gumid101.customercenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.Min;

import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.gumid101.crew.activity.CrewActivityBoardRepository;
import com.ssafy.gumid101.customexception.DuplicateException;
import com.ssafy.gumid101.customexception.IllegalParameterException;
import com.ssafy.gumid101.customexception.NotFoundUserException;
import com.ssafy.gumid101.dto.CrewBoardDto;
import com.ssafy.gumid101.dto.QuestionDto;
import com.ssafy.gumid101.dto.ReportDto;
import com.ssafy.gumid101.entity.CrewBoardEntity;
import com.ssafy.gumid101.entity.QUserEntity;
import com.ssafy.gumid101.entity.QuestionEntity;
import com.ssafy.gumid101.entity.ReportEntity;
import com.ssafy.gumid101.entity.UserEntity;
import com.ssafy.gumid101.req.QuestionReqDto;
import com.ssafy.gumid101.req.QuestionSelectParameter;
import com.ssafy.gumid101.req.ReportSelectReqDto;
import com.ssafy.gumid101.res.PagingParameter;
import com.ssafy.gumid101.res.QuestionResDto;
import com.ssafy.gumid101.res.ReportResDto;
import com.ssafy.gumid101.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCenterServiceImpl implements CustomerCenterService {

	private final ReportRepository reportRepository;
	private final QuestionRepository questionRepository;
	private final UserRepository userRepo;
	private final CrewActivityBoardRepository boardRepo;
	
	
	@Transactional
	@Override
	public QuestionDto postQuestion(QuestionDto questionDto, Long userSeq) throws Exception {
		UserEntity userEntity = userRepo.findById(userSeq)
				.orElseThrow(() -> new NotFoundUserException("유저 정보가 올바르지 않습니다."));
		QuestionEntity question = QuestionEntity.builder().questionContent(questionDto.getQuestionContent())
				.questionStatus(QuestionStatus.WAITING).userEntity(userEntity).build();

		questionRepository.save(question);

		return QuestionDto.of(question);
	}
	@Transactional
	@Override
	public ReportDto postReport(Long boardSeq, String reportContent, Long userSeq) throws Exception {
		UserEntity userEntity = userRepo.findById(userSeq)
				.orElseThrow(() -> new NotFoundUserException("유저 정보가 올바르지 않습니다."));

		if (reportRepository.findByUserReporterEntityAndReportCrewBoardSeq(userEntity, boardSeq).isPresent()) {
			throw new DuplicateException("이미 신고한 게시글입니다.");
		}

		CrewBoardEntity crewBoardEntity = boardRepo.findById(boardSeq)
				.orElseThrow(() -> new NotFoundUserException("해당 글이 존재하지 않습니다."));
		if (userEntity.getUserSeq() == crewBoardEntity.getUserEntity().getUserSeq()) {
			throw new IllegalParameterException("자기 자신의 글은 신고할 수 없습니다.");
		}

		ReportEntity reportEntity = ReportEntity.builder().reportContent(reportContent).reportCrewBoardSeq(boardSeq)
				.reportStatus(ReportStatus.WAITING).userReporterEntity(userEntity)
				.userTargetEntity(crewBoardEntity.getUserEntity()).build();

		reportRepository.save(reportEntity);

		return ReportDto.of(reportEntity);
	}
	
	@Transactional
	@Override
	public int deleteQuestion(Long questionSeq) throws Exception {
		try {
			questionRepository.deleteById(questionSeq);
		} catch (Exception e) {

			log.debug("신고글 삭제 에러 : {}", e.getMessage());
			return 0;
		}

		return 1;
	}
	@Transactional
	@Override
	public int answerQuestion(Long seq, QuestionReqDto questionReqDto, MultipartFile[] files) throws Exception {
		// question
		// 내일하자
		// TODO Auto-generated method stub

		return 0;
	}
	@Transactional
	@Override
	public Map<String, Object> selectQuestion(QuestionSelectParameter params) {

		List<QuestionResDto> questionList = questionRepository.selectQuestionByParam(params);
		long itemTotalCount = questionRepository.selectCountQuestionByParam(params);

		PagingParameter pagingParameter = new PagingParameter();
		// 전체 갯수 , 페이지 당 갯수 -1
		int pageNavSize = params.getPageNaviSize();
		int pageCurrent = params.getCurrentPage();
		int pageItemSize = params.getPageItemSize();

		int lastPageIndex = (int) ((itemTotalCount - 1) / pageItemSize) + 1;
		if (lastPageIndex <= 0) {
			lastPageIndex = 1;
		}

		int startPageIndex = (int) ((pageCurrent - 1) / pageNavSize) * pageNavSize + 1;
		int endPageIndex = (int) startPageIndex + pageNavSize - 1;
		int prevPageIndex = startPageIndex < 1 ? 1 : startPageIndex - 1;
		int nextPageIndex = endPageIndex >= lastPageIndex ? lastPageIndex : endPageIndex + 1;
		if (endPageIndex > lastPageIndex) {
			endPageIndex = lastPageIndex;
		}
		if (startPageIndex < 1) {
			startPageIndex = 1;
		}

		pagingParameter.setCurrentPageIndex(pageCurrent); // 현재 페이지
		pagingParameter.setEndPageIndex(endPageIndex);
		pagingParameter.setNextPageIndex(endPageIndex);
		pagingParameter.setPageNavSize(pageNavSize);
		pagingParameter.setPrevPageIndex(prevPageIndex);
		pagingParameter.setStartPageIndex(startPageIndex);

		Map<String, Object> map = new HashMap<>();
		map.put("qustions", questionList);
		map.put("pageinfo", pagingParameter);
		return map;

	}
	@Transactional
	@Override
	public int updateQustionStatus(Long questionSeq, QuestionStatus status) {
		Optional<QuestionEntity> question = questionRepository.findById(questionSeq);
		
		if(!question.isPresent()) {
			return 0;
		}
		
		question.get().setQuestionStatus(status);
		return 1;
	}


}
