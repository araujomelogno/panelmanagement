package uy.com.equipos.panelmanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

import jakarta.servlet.http.HttpServletResponse;
import uy.com.equipos.panelmanagement.views.login.LoginView;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    // Recomendado en prod (usa {bcrypt}, {noop}, etc.)
    @Bean
    public PasswordEncoder passwordEncoder() {
    	return new BCryptPasswordEncoder();
    }
    @Bean
    @Order(1)
    public SecurityFilterChain webhookChain(HttpSecurity http) throws Exception {
      // Matchea /api/webhook/** con o SIN context path (/utiles, /lo-que-sea)
      RequestMatcher r1 = new RegexRequestMatcher(".*/api/webhook/.*", null);
      RequestMatcher r2 = new AntPathRequestMatcher("/api/webhook/**");         // por si acaso
      RequestMatcher r3 = new AntPathRequestMatcher("/paneles/api/webhook/**");  // por si acaso
      
      
      
      RequestMatcher matcher = new OrRequestMatcher(r1, r2, r3);

      http
        .securityMatcher(matcher)
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(rc -> rc.disable())
        .formLogin(form -> form.disable())
        .logout(lo -> lo.disable())
        .exceptionHandling(eh -> eh.authenticationEntryPoint(
            (req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults());

      return http.build();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      // (si querés, dejá estáticos en permitAll)
      super.configure(http);
      setLoginView(http, LoginView.class);
    }
  }
