package com.groot.anil.service;

import java.util.Date;
import java.util.UUID;

import com.groot.anil.entity.User;
import com.groot.anil.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserServiceImp implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public User saveUser(User user, String url) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        String password = passwordEncoder.encode(user.getPassword());
        user.setPassword(password);
        user.setRole("ROLE_USER");
        user.setEnable(false);
        user.setVerificationCode(UUID.randomUUID().toString());
        user.setAccountNonLocked(true);
        user.setFailedAttempt(0);
        user.setLockTime(null);

        User savedUser = userRepository.save(user);
        if (savedUser != null) {
            sendEmail(savedUser, url);
        } else {
            throw new RuntimeException("Failed to save user");
        }

        return savedUser;
    }

    @Override
    public void removeSessionMessage() {
        HttpSession session = ((ServletRequestAttributes) (RequestContextHolder.getRequestAttributes())).getRequest()
                .getSession();
        session.removeAttribute("msg");
    }

    @Override
    public void sendEmail(User user, String url) {
        String from = "anilshebin6382@gmail.com";
        String to = user.getEmail();
        String subject = "Account Verification";
        String content = "Dear [[fullname]],<br>" +
                "Please click the link below to verify your registration:<br>" +
                "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>" +
                "Thank you,<br>" +
                "Your App Team";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setFrom(from, "Your App");
            helper.setTo(to);
            helper.setSubject(subject);

            content = content.replace("[[fullname]]", user.getFullname());
            String siteUrl = url + "/verify?code=" + user.getVerificationCode();
            content = content.replace("[[URL]]", siteUrl);

            helper.setText(content, true);

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean verifyAccount(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);
        if (user == null) {
            return false;
        } else {
            user.setEnable(true);
            user.setVerificationCode(null);
            userRepository.save(user);
            return true;
        }
    }

    @Override
    public void increaseFailedAttempt(User user) {
        int attempt = user.getFailedAttempt() + 1;
        userRepository.updateFailedtempt(attempt, user.getEmail());
    }

    @Override
    public void resetAttempt(String email) {
        userRepository.updateFailedtempt(0, email);
    }

    private static final long lock_duration_time = 30000; // 30 seconds for testing
    public static final long ATTEMT_TIME = 3;

    @Override
    public void lock(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());
        userRepository.save(user);
    }

    @Override
    public boolean unlockAccountTimeExpired(User user) {
        long lockTimeInMillis = user.getLockTime().getTime();
        long currentTimeMillis = System.currentTimeMillis();
        if (lockTimeInMillis + lock_duration_time < currentTimeMillis) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
