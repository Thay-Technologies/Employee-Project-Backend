package com.groot.anil.controller;

import java.security.Principal;

import com.groot.anil.entity.User;
import com.groot.anil.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private UserRepository userRepository;

    @ModelAttribute
    public void CommonUser(Principal p, Model model) {
        if (p != null) {
            String email = p.getName();
            User user = userRepository.findByEmail(email);
            model.addAttribute("user", user);
        }

    }

    @GetMapping("/profile")
    public String profile() {
        return "admin_profile";
    }
}
