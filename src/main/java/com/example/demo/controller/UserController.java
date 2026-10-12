package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Resource
    private UserMapper userMapper;

    @GetMapping("/list")
    public List<UserDTO> list(){
        List<User> users = userMapper.selectList(null);
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            result.add(toDTO(user));
        }
        return result;
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    @PostMapping
    public UserDTO add(@RequestBody User user){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));   // 存的是密文
        userMapper.insert(user);
        User user1 = userMapper.selectById(user.getId());
        return toDTO(user1);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user, HttpSession session){
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username",user.getUsername());
        User dbUser = userMapper.selectOne(wrapper);
        if(dbUser == null){
            return Map.of("success",false, "message", "用户名或密码错误");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if(encoder.matches(user.getPassword(), dbUser.getPassword())){
            session.setAttribute("userId", dbUser.getId());
            return Map.of("success", true, "message", "登录成功", "userId", dbUser.getId());
        }
        return Map.of("success",false, "message", "用户名或密码错误");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        userMapper.deleteById(id);
    }

    @PutMapping
    public User update(@RequestBody User user){
        userMapper.updateById(user);
        return user;
    }
}
