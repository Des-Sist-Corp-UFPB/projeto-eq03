package br.ufpb.dsc.chamados.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("appName", "Sistema de Chamados");
        model.addAttribute("appVersion", "1.0.0");
    }
}
