package com.vbs.demo.controller;

import com.vbs.demo.dto.DisplayDto;
import com.vbs.demo.dto.LoginDto;
import com.vbs.demo.dto.UpdateDto;
import com.vbs.demo.models.History;
import com.vbs.demo.models.Transaction;
import com.vbs.demo.models.User;
import com.vbs.demo.repositories.HistoryRepo;
import com.vbs.demo.repositories.TransactionRepo;
import com.vbs.demo.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    HistoryRepo historyRepo;

    @Autowired
    TransactionRepo transactionRepo;

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        try {
            User userExists = userRepo.findByEmail(user.getEmail());
            if (userExists != null) {
                return "Email already exists";
            }
            userRepo.save(user);
            return "Signup Successfull!";
        } catch (Exception e)
        {
            return e.getMessage();
        }
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginDto u)
    {
        User user = userRepo.findByUsername(u.getUsername());

        if (user==null)
        {
            return "User Not Found!";
        }
        if (!u.getPassword().equals(user.getPassword()))
        {
            return "Password is Incorrect";
        }
        if (!u.getRole().equals(user.getRole()))
        {
            return "Role is Incorrect";
        }

        return String.valueOf(user.getId());
    }

    @GetMapping("/get-details/{id}")
    public DisplayDto display(@PathVariable int id)
    {
        User user = userRepo.findById(id).
                orElseThrow(() -> new RuntimeException("User not found"));
        DisplayDto displayDto = new DisplayDto();
        displayDto.setUsername(user.getUsername());
        displayDto.setBalance(user.getBalance());
        return displayDto;

    }

    @PostMapping("/update")
    public String update(@RequestBody UpdateDto obj)
    {
        User user = userRepo.findById(obj.getId())
                .orElseThrow(()-> new RuntimeException("Invalid ID"));

        History h2 = new History();

        if(obj.getKey().equalsIgnoreCase("name"))
        {
            if(obj.getValue().equals(user.getName())) return "new name cannot be same";
            user.setName(obj.getValue());
            h2.setDescription("User "+user.getUsername()+" changed name to "+obj.getValue());
        }

        else if(obj.getKey().equalsIgnoreCase("password"))
        {
            if(obj.getValue().equals(user.getPassword())) return "new password cannot be same";
            user.setPassword(obj.getValue());
            h2.setDescription("User "+user.getUsername()+" changed password to "+obj.getValue());
        }

        else if(obj.getKey().equalsIgnoreCase("email"))
        {
            if(obj.getValue().equals(user.getEmail())) return "new email cannot be same";
            User user2 = userRepo.findByEmail(obj.getValue());
            if(user2!=null) return "Email already exists";
            user.setEmail(obj.getValue());
            h2.setDescription("User "+user.getUsername()+" changed email to "+obj.getValue());
        }

        else
        {
            return "Invalid Key";
        }

        historyRepo.save(h2);
        userRepo.save(user);

        return "Update Successfully";
    }

    @PostMapping("/add/{adminId}")
    public ResponseEntity<String> add(@RequestBody User user,@PathVariable int adminId)
    {
        try {
            User userExists = userRepo.findByEmail(user.getEmail());
            if (userExists!=null)
            {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists!");
            }
            History h1 = new History();
            h1.setDescription("Admin "+adminId+" Created user "+user.getUsername());
            userRepo.save(user);

            if(user.getBalance()>0) {
                User user2 = userRepo.findByUsername(user.getUsername());
                Transaction t = new Transaction();
                t.setAmount(user.getBalance());
                t.setCurrBalance(user.getBalance());
                t.setDescription("Rs " + user.getBalance()+" Deposit Successful");
                t.setUserId(user2.getId());
                transactionRepo.save(t);
            }

            historyRepo.save(h1);
            return ResponseEntity.ok("User Created Successfully!");
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Backend Error: " + e.getMessage());
        }
    }

    @DeleteMapping("delete-user/{userId}/admin/{adminId}")
    public String delete(@PathVariable int userId, @PathVariable int adminId)
    {
        User user = userRepo.findById(userId).orElseThrow(()->new RuntimeException("Not found"));
        if(user.getBalance()>0)
        {
            return "Balance should be zero";
        }
        History h1 = new History();
        h1.setDescription("Admin "+adminId+" Deleted User "+user.getUsername());
        historyRepo.save(h1);
        userRepo.delete(user);
        return "User Deleted Successfully";
    }

    @GetMapping("/users")
    public List<User> getAllUsers(@RequestParam String sortBy, @RequestParam String order)
    {
        Sort sort;
        if(order.equalsIgnoreCase("desc"))
        {
            sort = Sort.by(sortBy).descending();
        }
        else
        {
            sort=Sort.by(sortBy).ascending();
        }
        return userRepo.findAllByRole("customer",sort);
    }

    @GetMapping("/users/{keyword}")
    public List<User> getUser(@PathVariable String keyword)
    {
        return userRepo.findByUsernameContainingIgnoreCaseAndRole(keyword,"customer");
    }
}
