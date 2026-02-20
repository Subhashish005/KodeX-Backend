package com.something.kodex_backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

  @GetMapping("/public/home")
  public String greet() {
    return "hola!";
  }

   // example code for request param
   // type of request allowed: /public/example?query1={value1}&query2={value2}
//  @GetMapping("/public/example")
//  public String printQueryFromParameter(
//    @RequestParam("query1") String value1,
//    @RequestParam("query2") String value2
//  ) {
//    return value1 + " " + value2;
//  }

  // example code for path variable
  // type of request allowed: /public/example/{value}
//  @GetMapping("/public/example/{value}")
//  public String printValue(
//    @PathVariable("value") String value
//  ) {
//    return "Path variable is: " + value;
//  }

  @GetMapping("/home")
  public String hello() {
    return "hola! from secured endpoint :)";
  }

}