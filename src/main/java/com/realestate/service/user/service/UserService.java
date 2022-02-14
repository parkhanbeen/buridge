package com.realestate.service.user.service;

import java.io.IOException;
import java.util.Random;

import com.realestate.service.user.constant.Role;
import com.realestate.service.user.constant.Status;
import com.realestate.service.user.dto.UserEmailDto;
import com.realestate.service.user.dto.UserSignupDto;
import com.realestate.service.user.entity.User;
import com.realestate.service.user.repository.UserRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Value("${spring.sendgrid.api-key}")
  private String sendGridApiKey;



  /**.
   * 회원가입 : 이메일, 비밀번호를 입력하면 회원가입
   * */
  @Transactional
  public void signup(UserSignupDto userSignupDto) {

    // 비밀번호 암호화 처리
    String encodedPassword = passwordEncoder.encode(userSignupDto.getPassword());

    log.info("암호화된 비밀번호 : " + encodedPassword);


    //User 객체 빌드하기
    User user = User.builder()
        .email(userSignupDto.getEmail())
        .password(encodedPassword)
        .nickName(userSignupDto.getNickName())
        .status(Status.ACTIVE)
        .role(Role.NORMAL)
        .build();

    log.info("User 이메일 : " + user.getEmail());
    log.info("User 비밀번호 : " + encodedPassword);
    log.info("User 닉네임 : " + user.getNickName());
    log.info("User 상태 : " + user.getStatus());
    log.info("User 권한 : " + user.getRole());


    userRepository.save(user);
  }

  /**.
   * 인증번호 전송 : 이메일 주소로 인증번호 전송.
   * */
  public void sendValidationCode(UserEmailDto userEmailDto) {

    /** 인증번호 6자리 발행 */
    int secretCode = generateSecretCode(userEmailDto);

    /** 인증번호 이메일 전송 */
    sendEmail(userEmailDto, secretCode);

  }

  /**.
   * 인증코드 발행 : 비밀번호 변경을 위한 인증코드 발행
   * */
  public int generateSecretCode(UserEmailDto userEmailDto) {

    /** 6자리 난수 발생 */
    Random random = new Random();
    int secretCode = random.nextInt(1000000);

    /** 이메일로 사용자 조회 */
    User user = userRepository.findUserByEmail(userEmailDto.getEmail())
                          .orElseThrow(() -> new IllegalArgumentException(
                              String.format("회원정보가 존재하지 않습니다.[이메일 : %s]", userEmailDto.getEmail())
                          ));


    /** 사용자의 인증코드 업데이트 */
    user.updateUserValidationCode(secretCode);

    return secretCode;
  }

  /**.
   * 이메일 전송 : 사용자에게 비밀번호 인증코드 이메일 전송
   * */
  public void sendEmail(UserEmailDto userEmailDto, int secretCode) {

    /** 이메일 객체 생성 : 송신자, 수신자, 제목, 이메일 내용 */
    Email fromEmail = new Email("sendgrid API에 등록된 이메일 주소 --> 상수값으로 뺼 예정");
    Email toEmail = new Email(userEmailDto.getEmail());
    String subject = "@@@@@ Buridge password validation code @@@@@@";
    String txt = "Validation code is " + secretCode;
    Content content = new Content("text/plain", txt);
    Mail mail = new Mail(fromEmail, subject, toEmail, content);

    /** 이메일 전송 */
    SendGrid sg = new SendGrid(sendGridApiKey);
    Request request = new Request();
    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());
      Response response = sg.api(request);

      log.info(String.valueOf(response.getStatusCode()));
      log.info(response.getBody());
      log.info(String.valueOf(response.getHeaders()));
      log.info(userEmailDto.getEmail() + " 비밀번호 인증코드 전송완료");

    } catch (IOException e) {
      log.info("SendGrid IO Exception");
      e.printStackTrace();
    }


  }



}
