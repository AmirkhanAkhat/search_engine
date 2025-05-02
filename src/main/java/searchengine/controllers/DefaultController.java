package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     *
     * --- Translation ---
     *The method generates a page from the HTML file index.html,
     * which is located in the resources/templates folder.
     * This is done by the Thymeleaf library.
     */

    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
