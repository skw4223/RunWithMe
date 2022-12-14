package com.ssafy.gumid101.OAuth.custom.validate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ssafy.gumid101.achievement.AchievementRepository;
import com.ssafy.gumid101.crew.manager.CrewManagerService;
import com.ssafy.gumid101.customexception.FCMTokenUnValidException;
import com.ssafy.gumid101.dto.AchievementDto;
import com.ssafy.gumid101.dto.UserDto;
import com.ssafy.gumid101.entity.AchievementEntity;
import com.ssafy.gumid101.entity.UserEntity;
import com.ssafy.gumid101.firebase.FcmMessage;
import com.ssafy.gumid101.firebase.FcmMessage.Message;
import com.ssafy.gumid101.firebase.FirebaseMessageUtil;
import com.ssafy.gumid101.jwt.JwtUtilsService;
import com.ssafy.gumid101.redis.RedisService;
import com.ssafy.gumid101.res.ResponseFrame;
import com.ssafy.gumid101.schedule.CrewSchedule;
import com.ssafy.gumid101.user.Role;
import com.ssafy.gumid101.user.UserRepository;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class AK {
	String code;
}

@Controller
public class TestController {

	
	@Autowired
	private CrewSchedule crewSchedule;
	
	@Autowired

	private UserRepository userRepo;
	@Autowired
	private CrewManagerService cmServ;
	@Autowired
	private JwtUtilsService jwtUtilSevice;

	@Autowired
	private FirebaseMessageUtil firebaseMessage;
	


	@Autowired
	private AchievementRepository achRepo;
	@Autowired
	private RedisService redisServ;
	
	@Autowired
	private CrewSchedule cs;
	@ApiOperation(value = "?????? ?????? ?????? ?????????")
	@GetMapping("/batchtest")
	public ResponseEntity<?> batchtest() throws Exception {


		cs.crewPointDistribute();
		ResponseFrame<?> res = ResponseFrame.of(true, 0, "????????? ?????? ??????");

		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@ApiOperation(value = "?????? ????????????")
	@GetMapping("/{crewSeq}/distribute")
	public ResponseEntity<?> crewMemberCheck(@PathVariable Long crewSeq) throws Exception {

		Boolean check = cmServ.crewFinishPoint(crewSeq);

		ResponseFrame<?> res = ResponseFrame.of(check, 0, "?????? ??????????????? ??????");

		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@ResponseBody
	@PostMapping("/test/register")
	public ResponseEntity<?> register(@ModelAttribute UserDto userDto) {
		UserEntity user = UserEntity.builder().email(userDto.getEmail()).nickName(userDto.getNickName())
				.height(userDto.getHeight()).weight(userDto.getWeight()).build();

		userRepo.save(user);

		userDto.setRole(user.getRole());
		userDto.setUserSeq(user.getUserSeq());

		String token = jwtUtilSevice.createToken(userDto);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("msg", "???????????? ????????????");
		map.put("j-token-develope", token);
		return new ResponseEntity<>(map, org.springframework.http.HttpStatus.OK);
	}

	@ResponseBody
	@PostMapping("/test/register/temp")
	public ResponseEntity<?> register(@RequestParam String email) {

		UserEntity user = UserEntity.builder().email(email).role(Role.TEMP).build();

		String token = jwtUtilSevice.createToken(UserDto.of(user));
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("msg", "???????????? ????????????");
		map.put("j-token-develope", token);
		return new ResponseEntity<>(map, org.springframework.http.HttpStatus.OK);
	}

	
	@ResponseBody
	@ApiOperation(value="?????? ????????????")
	@PostMapping("/test/achievement")
	public ResponseEntity<?> addAchievement(@ModelAttribute AchievementDto achievementDto){
		AchievementEntity achievementEntity = AchievementEntity.builder()
				.achieveName(achievementDto.getAchieveName())
				.achieveType(achievementDto.getAchieveType())
				.achiveValue(achievementDto.getAchieveValue())
				.build();
		
		achRepo.save(achievementEntity);
		
		return new ResponseEntity<>(new ResponseFrame<>(true, AchievementDto.of(achievementEntity), 1, "???????????????"), HttpStatus.OK);
	}
	
	@ResponseBody
	@ApiOperation(value="????????? ?????????")
	@PostMapping("/test/redistest")
	public ResponseEntity<?> getRedisStringValue(@RequestParam String key) throws Exception{
		redisServ.getIsUseable(key, 15);
		
		return new ResponseEntity<>(new ResponseFrame<>(true, null, 1, "??????????????????"), HttpStatus.OK);
	}
	
	
	@PostMapping("/test")
	public String tset(@ModelAttribute AK a) throws GeneralSecurityException, IOException {
		Map map = new GoogleTokenValidate().validate(a.code);

		return "???";
	}

	@ResponseBody
	@GetMapping("/test/get/{userId}")
	public String tset(@PathVariable Long userId) {

		System.out.println("kkk");

		return jwtUtilSevice.createToken(UserDto.of(userRepo.findById(userId).orElse(null)));
	}

	
	@ResponseBody
	@GetMapping("/test/notification")
	public String notificationTest() throws IOException{
		
		List<Message> fcmMesageList = userRepo.findAll().stream().map((userEntity)->{
			return FcmMessage.ofMessage(userEntity.getFcmToken(), "?????????", "???????????????\n ??????");
		}).collect(Collectors.toList());
		
		firebaseMessage.sendMessageTo(fcmMesageList);
		
		return "???????????????";
	}
	
	@ApiParam(name = "?????? ???????????? ????????? ???????????? ????????????.")
	@ResponseBody
	@GetMapping("/test/fcm")
	public String notifitoUser(@RequestParam Long userSeq) throws IOException, FCMTokenUnValidException {
		String token =  userRepo.findById(userSeq).get().getFcmToken();
		firebaseMessage.sendMessageTo(token, "?????????", "????????? ????????? ??????");
		return "gg";
	}
	
	@ApiOperation("?????? ????????? ?????? 08 - 12")
	@GetMapping("/test/distriute")
	private String crewPointDestribute() throws Exception {
		crewSchedule.crewPointDistribute();
		return "asdf";
	}
	
	
	@ApiOperation("???????????? ????????? ?????? 08 - 12")
	@GetMapping("/test/competitionPointDistribute")
	private String competitionPointDistribute() throws Exception {
		crewSchedule.competitionPointDistribute();
		return "asdf";
	}
	
	@ApiOperation("?????? ??????  08 - 12")
	@GetMapping("/test/crewPointAlarm")
	private String crewPointAlarm() throws Exception {
		crewSchedule.notification();
		return "asdf";
	}
}
