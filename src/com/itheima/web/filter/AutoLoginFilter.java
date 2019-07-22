package com.itheima.web.filter;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itheima.domain.User;
import com.itheima.service.UserService;

public class AutoLoginFilter implements Filter{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		
		User user = (User) req.getSession().getAttribute("user");
		if(user==null){
			String cookie_username = null;
			String cookie_password = null;
			//获取携带用户名和密码的cookie
			Cookie[] cookies = req.getCookies();
			if(cookies!=null){
				for(Cookie cookie:cookies){
					//获得想要的cookie
					if(cookie.getName().equals("cookie_username")){
						cookie_username = cookie.getValue();
					}
					if(cookie.getName().equals("cookie_password")){
						cookie_password = cookie.getValue();
					}
				}
			}
			if(cookie_username!=null&&cookie_password!=null){
				//校验用户名和密码是否正确
				UserService service = new UserService();
				try {
					user = service.userLogin(cookie_username,cookie_password);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				//完成自动登陆
				req.getSession().setAttribute("user", user);
			}
		}
		//放行
		chain.doFilter(req, res);
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	

}
