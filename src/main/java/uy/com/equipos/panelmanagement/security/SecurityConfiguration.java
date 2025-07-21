package uy.com.equipos.panelmanagement.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Asegúrate de importar esto
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import uy.com.equipos.panelmanagement.views.login.LoginView;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());

        // Icons from the line-awesome addon
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll());

        // Permitir acceso anónimo a tu endpoint de webhook para solicitudes POST
        // y deshabilitar CSRF para esta ruta específica.
        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/webhook/**")));
        
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/api/webhook/survey-response", HttpMethod.POST.toString())).permitAll());
        
        

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/api/webhook/landing-response", HttpMethod.POST.toString())).permitAll());
        
        super.configure(http); // Llamada a la configuración base de VaadinWebSecurity
        setLoginView(http, LoginView.class); // Configura tu vista de login
    }

}