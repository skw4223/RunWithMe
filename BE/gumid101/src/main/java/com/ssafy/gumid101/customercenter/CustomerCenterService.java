package com.ssafy.gumid101.customercenter;

import com.ssafy.gumid101.dto.QuestionDto;
import com.ssafy.gumid101.dto.ReportDto;

public interface CustomerCenterService {

	QuestionDto postQuestion(QuestionDto questionDto, Long userSeq) throws Exception;

	ReportDto postReport(long boardSeq, String report_content, Long userSeq) throws Exception;

}