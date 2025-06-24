package es.codeurjc.helloword_vscode.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import es.codeurjc.helloword_vscode.security.jwt.CustomAccessDeniedHandler;
import es.codeurjc.helloword_vscode.security.jwt.JwtRequestFilter;
import es.codeurjc.helloword_vscode.security.jwt.UnauthorizedHandlerJwt;
import es.codeurjc.helloword_vscode.service.MemberService;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
	private UnauthorizedHandlerJwt unauthorizedHandlerJwt;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;
    
    /* Bean for password encoding */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

    @Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

    @Bean
    public UserDetailsService userDetailsService(MemberService memberService) {
        return username -> memberService.loadUserByUsername(username);
    }



    /* Bean for authentication provider */
	@Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }


    /* Bean for security filter chain */
    @Bean
	@Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, DaoAuthenticationProvider authProvider, JwtRequestFilter jwtRequestFilter) throws Exception {
		
		http.authenticationProvider(authProvider);
		
		http
			.securityMatcher("/api/**")
			.exceptionHandling(handling -> handling
                .authenticationEntryPoint(unauthorizedHandlerJwt)
                .accessDeniedHandler(accessDeniedHandler)
            );
		
		http
			.authorizeHttpRequests(authorize -> authorize
                    // PRIVATE ENDPOINTS
                    .requestMatchers(HttpMethod.POST,"/api/associations/**").hasRole("USER")
                    .requestMatchers(HttpMethod.PUT,"/api/associations/**").hasRole("USER")
                    .requestMatchers(HttpMethod.DELETE,"/api/associations/**").hasRole("ADMIN")

                    .requestMatchers(HttpMethod.PUT,"/api/members/**").hasRole("USER")
                    .requestMatchers(HttpMethod.DELETE,"/api/members/**").hasRole("USER")

                    .requestMatchers(HttpMethod.POST, "/api/memberTypes/").hasRole("USER")
                    .requestMatchers(HttpMethod.PUT, "/api/memberTypes/**").hasRole("USER")
                    .requestMatchers(HttpMethod.DELETE, "/api/memberTypes/**").hasRole("USER")

                    .requestMatchers(HttpMethod.POST, "/api/minutes/**").hasRole("USER")
                    .requestMatchers(HttpMethod.PUT, "/api/minutes/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/minutes/**").hasRole("ADMIN")

					// PUBLIC ENDPOINTS
					.anyRequest().permitAll()
			);
		
        // Disable Form login Authentication
        http.formLogin(formLogin -> formLogin.disable());

        // Disable CSRF protection (it is difficult to implement in REST APIs)
        http.csrf(csrf -> csrf.disable());

        // Disable Basic Authentication
        http.httpBasic(httpBasic -> httpBasic.disable());

        // Stateless session
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// Add JWT Token filter
		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
    @Order(2)
    // Configure method with http object for security
    public SecurityFilterChain webFilterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        // Set the authentication provider
        http.authenticationProvider(authProvider);
    
        // Configure authorization rules
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public pages
                .requestMatchers(
                    "/images/**",
                    "/search/**", 
                    "/members",
                    "/css/**",
                    "/",
                    "/association/*", 
                    "/user/**", 
                    "/profile/create", 
                    "/login/create",
                    "/association/*/image",
                    "/api/**"
                ).permitAll()
                
                 // Pages accessible to users with role "USER"
                .requestMatchers(
                    "/profile", 
                    "/association/*/join",
                    "/profile/update",
                    "/profile/edit", 
                    "/profile/delete/confirm",
                    "/profile/delete", 
                    "/association/*/createMinute",
                    "/association/*/new_minute",
                    "/association/*/leave",
                    "/association/*/changeRole"
                ).hasAnyRole("USER")

                // Pages accessible to users with role "ADMIN"
                .requestMatchers(
                    "/association/create",
                    "/association/*/delete",
                    "/association/*/deleteImage",
                    "/editasso",
                    "/editasso/**",
                    "/createasso",
                    "/profile/*/delete",
                    "/minute/*/asso/*/edit",
                    "/editminute",
                    "/minute/*/asso/*/delete"
                ).hasAnyRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Configure form login
            .formLogin(formLogin -> formLogin
                .loginPage("/login") // Custom login page
                .failureUrl("/loginerror") // Redirect on login failure
                .defaultSuccessUrl("/") // Redirect on login success
                .permitAll() // Allow all users to access the login page
            )

            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/logout") // URL to trigger logout
                .logoutSuccessUrl("/") // Redirect on logout success
                .invalidateHttpSession(true) // Invalidate the HTTP session
                .deleteCookies("JSESSIONID") // Delete the JSESSIONID cookie
                .permitAll() // Allow all users to access the logout functionality
            )

            // Configure session management
            .sessionManagement(session -> session
                .sessionFixation().newSession()  // Create new session after login
                .maximumSessions(1)  // Authorize only one session per user
                .expiredUrl("/login?expired")  // Redirect on login if the session died
            );
    
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );

    
        // Build and return the security filter chain
        return http.build();
    }
    
}
