package api.services;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import api.dto.UserRequestDto;
import api.dto.UserResponseDto;
import jakarta.validation.Valid;

public interface UsersService {

	@GetMapping("/users/auth-list")
	ResponseEntity<List<UserRequestDto>> getUsersForAuth(); // bez request header
	
	@GetMapping("/users")
	List<UserResponseDto> getUsers(@RequestHeader("Authorization") String authorizationHeader);
	
	@PostMapping("/users/newUser")
	ResponseEntity<?> createUser(@Valid @RequestBody UserRequestDto dto, @RequestHeader("Authorization") String authorizationHeader);
	
	@PutMapping("/users/{id}")
	ResponseEntity<?> updateUser(@PathVariable int id, @Valid @RequestBody UserRequestDto dto, @RequestHeader("Authorization") String authorizationHeader);

	@DeleteMapping("/users/{id}")
	ResponseEntity<?> deleteUser(@PathVariable int id,@RequestHeader("Authorization") String authorizationHeader);

	@GetMapping("/users/current-user-role")
	String getCurrentUserRole(@RequestHeader("Authorization") String authorizationHeader);
	
	@GetMapping("/users/current-user-email")
	String getCurrentUserEmail(@RequestHeader("Authorization") String authorizationHeader);

	@GetMapping("/users/by-email/{email}") // /users/email/{email}
	Boolean getUser(@PathVariable("email") String email);
}
