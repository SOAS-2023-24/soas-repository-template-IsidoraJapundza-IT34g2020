package api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRequestDto {

	@Email
	@NotBlank
	private String email;
	
	@NotBlank
	//@Size(min = 6, max = 60)
	private String password; 
	
	@NotBlank
	@Pattern(regexp = "ADMIN|USER|OWNER", message = "Role must be ADMIN, USER, OWNER")
	private String role;
	
	public UserRequestDto() {
		
	}
	
	public UserRequestDto(String email, String password, String role) {
		this.email = email;
		this.password = password;
		this.role = role;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getRole() {
		return role;
	}
	
	public void setRole(String role) {
		this.role = role;
	}
}