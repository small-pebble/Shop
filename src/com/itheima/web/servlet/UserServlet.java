package com.itheima.web.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.itheima.domain.User;
import com.itheima.service.UserService;

public class UserServlet extends BaseServlet {

	//用户登录
	public void userLogin(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();
		//获得用户名和密码
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		//将用户名和密码传递到service层
		UserService service  = new UserService();
		User user = null;
		try {
			user = service.userLogin(username,password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(user!=null){
			//登陆成功
			//判断用户是否勾选自动登陆
			String autoLogin = request.getParameter("autoLogin");
			if(autoLogin!=null){
				//要自动登陆---创建储存用户名的cookie
				Cookie cookie_username = new Cookie("cookie_username",user.getUsername());
				cookie_username.setMaxAge(10*60);
				Cookie cookie_password = new Cookie("cookie_password",user.getPassword());
				cookie_password.setMaxAge(10*60);
				
				response.addCookie(cookie_username);
				response.addCookie(cookie_password);
				
			}
			//将user对象存到session中
			session.setAttribute("user",user);
			//重定向到首页
			response.sendRedirect(request.getContextPath() + "/index.jsp");
		}else{
			request.setAttribute("loginError", "用户名或密码错误");
			request.getRequestDispatcher("login.jsp").forward(request, response);
		}
		
	}
}
