package com.itheima.web.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;

import com.google.gson.Gson;
import com.itheima.domain.Cart;
import com.itheima.domain.CartItem;
import com.itheima.domain.Category;
import com.itheima.domain.Order;
import com.itheima.domain.OrderItem;
import com.itheima.domain.PageBean;
import com.itheima.domain.Product;
import com.itheima.domain.User;
import com.itheima.service.ProductService;
import com.itheima.utils.CommonsUtils;
import com.itheima.utils.JedisPoolUtils;
import com.itheima.utils.PaymentUtil;

import redis.clients.jedis.Jedis;

public class ProductServlet extends BaseServlet {
	
	//获得当前用户的订单
	public void myOrders(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//判断用户是否登陆
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		
		ProductService service = new ProductService();
		//查询该用户的订单（单表查询orders）
		List<Order> orderList = service.findAllOrdersByUid(user.getUid());
		//循环所有订单---找到每个订单的订单项
		if(orderList != null){
			for(Order order:orderList){
				List<Map<String,Object>> mapList = service.findAllOrderItemsByOid(order.getOid());
				//将Map<String,Object>转换成List<OrderItem>
				for(Map<String,Object> map:mapList){
					try {
						OrderItem item = new OrderItem();
						//从Map中取出OrderItem的属性封装到item中
						BeanUtils.populate(item, map);
						//从Map中取出Product的属性封装到product中
						Product product = new Product();
						BeanUtils.populate(product, map);
						//将product封装到OrderItem中
						item.setProduct(product);
						//将item封装到Order中的orderItems中
						order.getOrderItems().add(item);
					} catch (IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}
		//orderList封装完成
		request.setAttribute("orderList",orderList);
		request.getRequestDispatcher("/order_list.jsp").forward(request, response);
		
	}

	//确认订单---更新收货人信息+在线支付
	public void confirmOrder(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//1.更新收货人信息
		Map<String,String[]> parameters = request.getParameterMap();
		Order order = new Order();
		try {
			BeanUtils.populate(order, parameters);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		ProductService service = new ProductService();
		service.updateOrderAddr(order);
		
		// 2.在线支付
		// 获得 支付必须基本数据
		String orderid = request.getParameter("oid");
		String money = "0.01";//支付金额
		// 银行
		String pd_FrpId = request.getParameter("pd_FrpId");

		// 发给支付公司需要哪些数据
		String p0_Cmd = "Buy";//业务类型
		String p1_MerId = ResourceBundle.getBundle("merchantInfo").getString("p1_MerId");
		String p2_Order = orderid;//
		String p3_Amt = money;
		String p4_Cur = "CNY";
		String p5_Pid = "";
		String p6_Pcat = "";
		String p7_Pdesc = "";
		// 支付成功回调地址 ---- 第三方支付公司会访问、用户访问
		// 第三方支付可以访问网址
		String p8_Url = ResourceBundle.getBundle("merchantInfo").getString("callback");
		String p9_SAF = "";
		String pa_MP = "";
		String pr_NeedResponse = "1";
		// 加密hmac 需要密钥
		String keyValue = ResourceBundle.getBundle("merchantInfo").getString("keyValue");
		String hmac = PaymentUtil.buildHmac(p0_Cmd, p1_MerId, p2_Order, p3_Amt, p4_Cur, p5_Pid, p6_Pcat, p7_Pdesc,
				p8_Url, p9_SAF, pa_MP, pd_FrpId, pr_NeedResponse, keyValue);

		String url = "https://www.yeepay.com/app-merchant-proxy/node?pd_FrpId=" + pd_FrpId + "&p0_Cmd=" + p0_Cmd
				+ "&p1_MerId=" + p1_MerId + "&p2_Order=" + p2_Order + "&p3_Amt=" + p3_Amt + "&p4_Cur=" + p4_Cur
				+ "&p5_Pid=" + p5_Pid + "&p6_Pcat=" + p6_Pcat + "&p7_Pdesc=" + p7_Pdesc + "&p8_Url=" + p8_Url
				+ "&p9_SAF=" + p9_SAF + "&pa_MP=" + pa_MP + "&pr_NeedResponse=" + pr_NeedResponse + "&hmac=" + hmac;

		// 重定向到第三方支付平台
		response.sendRedirect(url);
	}

	//提交订单
	public void submitOrder(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();
		//判断用户是否登陆
		User user = (User)session.getAttribute("user");
		if(user==null){
			//用户没有登陆
			response.sendRedirect(request.getContextPath()+"/login.jsp");
			return;
		}
		//封装order
		Order order = new Order();
		//该订单号
		//1.private String oid;
		String oid = CommonsUtils.getUUID();
		order.setOid(oid);
		//下单时间
		//2.private Date ordertiem;
		order.setOrdertime(new Date());
		//订单金额
		//3.private double total;
		Cart cart = (Cart) session.getAttribute("cart");
		if(cart!=null){
			order.setTotal(cart.getTotal());
		}
		//订单支付状态，1代表已付款
		//4.private int state;
		order.setState(0);
		//收获地址
		//5.private String address;
		order.setAddress(null);
		//收货人
		//6.private String name;
		order.setName(null);
		//收货人电话
		//7.private String telephone;
		order.setTelephone(null);
		
		//该订单所属用户
		//8.private User user;
		order.setUser(user);
		
		//封装该订单的List<OrderItem>
		Map<String,CartItem> cartItems = cart.getCartItems();
		for(Map.Entry<String, CartItem> entry:cartItems.entrySet()){
			//创建订单项
			OrderItem orderItem = new OrderItem();
			//取出购物项
			CartItem cartItem = entry.getValue();
			//订单项标识
			//1.private String itemid;
			orderItem.setItemid(CommonsUtils.getUUID());
			//该商品购买数量
			//2.private int count;
			orderItem.setCount(cartItem.getBuyNum());
			//该商品小计
			//3.private double subtotal;
			orderItem.setSubtotal(cartItem.getSubTotal());
			//该商品
			//4.private Product product;
			orderItem.setProduct(cartItem.getProduct());
			//所属订单
			//private Order order;
			orderItem.setOrder(order);
			
			//将该订单项添加到order的List<OrderItem>
			order.getOrderItems().add(orderItem);
		}
		
		//传递数据到service
		ProductService service = new ProductService();
		service.submitOrder(order);
		
		session.setAttribute("order", order);
		
		response.sendRedirect(request.getContextPath() + "/order_info.jsp");
	}
	
	//清空购物车
	public void claerCart(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();
		session.removeAttribute("cart");
		response.sendRedirect(request.getContextPath() + "/cart.jsp");
	}
	
	
	//删除购物项
	public void deleteCartItemByPid(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//获得要删除商品的pid
		String pid = request.getParameter("pid");
		//获得购物项集合
		HttpSession session = request.getSession();
		Cart cart = (Cart) session.getAttribute("cart");
		Map<String,CartItem> cartItems = cart.getCartItems();
		//将总价减去该购物项小计
		double total = cart.getTotal() - cartItems.get(pid).getSubTotal();
		cart.setTotal(total);
		//删除该购物项
		cartItems.remove(pid);
		//重定向到购物车页面
		response.sendRedirect(request.getContextPath() + "/cart.jsp");
		
		
	}
	//添加到购物车
	public void addProductToCart(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		HttpSession session = request.getSession();
		
		ProductService service = new ProductService();
		
		//获得要添加到购物车的商品pid和数量
		String pid = request.getParameter("pid");
		int buyNum = Integer.parseInt(request.getParameter("buyNum"));
		
		//通过pid获得商品
		Product product = service.findProductByPid(pid);
		//封装成购物项
		CartItem item = new CartItem();
		item.setProduct(product);
		item.setBuyNum(buyNum);
		double subTotal = product.getShop_price()*buyNum;
		item.setSubTotal(subTotal);
		
		//获得购物车---判断session中是否存在购物车
		Cart cart = (Cart) session.getAttribute("cart");
		if(cart==null){
			cart = new Cart();
		}
		
		//将购物项添加到cart---key是pid
		//先判断购物车中是否已经包含此购物项---判断pid是否已经存在
		//如果购物车中已存在该商品--将购买数量相加
		//获得购物车中的购物项集合
		Map<String,CartItem> cartItems = cart.getCartItems();
		double oldTotal = cart.getTotal();
		if(cartItems.containsKey(pid)){
			//包含该商品
			//取出原有商品的数目
			CartItem cartItem = cartItems.get(pid);
			int oldBuyNum = cartItem.getBuyNum();
			int newBuyNum = oldBuyNum + buyNum;
			cartItem.setBuyNum(newBuyNum);
			//修改小计
			cartItem.setSubTotal(newBuyNum*product.getShop_price());
			cart.setTotal(oldTotal+buyNum*product.getShop_price());
		}else{
			cart.getCartItems().put(product.getPid(), item);
			cart.setTotal(oldTotal+buyNum*product.getShop_price());
		}
		
		//将车再次放入session中
		session.setAttribute("cart", cart);
		
		//重定向到购物车页面
		response.sendRedirect(request.getContextPath()+"/cart.jsp");
		
	}
	
	
	//显示分类
	public void categoryList(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ProductService service = new ProductService();

		// 先从缓存中查询categoryList，如果有直接使用，没有再从数据库中查询存到缓存中
		// 1.获得jedis对象，连接redis数据库
		Jedis jedis = JedisPoolUtils.getJedis();
		String categoryListJson = jedis.get("categoryListJson");
		// 2.判断categoryListJson是否为空
		if (categoryListJson == null) {
			System.out.println("缓存中没有数据，从数据库中查询");
			// 准备分类数据
			List<Category> categoryList = service.findAllCategory();
			Gson gson = new Gson();
			categoryListJson = gson.toJson(categoryList);
			jedis.set("categoryListJson", categoryListJson);
		}
		//获得分类数据
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write(categoryListJson);
	}
	

	//首页商品
	public void index(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		ProductService service = new ProductService();
		
		//准备热门商品
		List<Product> hotProductList = service.findHotProductList();
		//准备最新商品
		List<Product> newProductList = service.findNewProductList();
		//准备分类数据
		//List<Category> categoryList = service.findAllCategory();
		//request.setAttribute("categoryList", categoryList);
		request.setAttribute("hotProductList", hotProductList);
		request.setAttribute("newProductList", newProductList);
		
		request.getRequestDispatcher("/index.jsp").forward(request, response);
		
	}
	
	
	//商品详细信息
	public void productInfo(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		//获得商品cid
		String cid = request.getParameter("cid");
		//获得商品当前页
		String currentPage = request.getParameter("currentPage");
		//获得pid
		String pid = request.getParameter("pid");
		
		ProductService service = new ProductService();
		Product product = service.findProductByPid(pid);
		
		request.setAttribute("product", product);
		request.setAttribute("cid", cid);
		request.setAttribute("currentPage",currentPage);
		
		//获得客户端携带的cookie，名字为pids
		String pids = pid;
		Cookie[] cookies = request.getCookies();
		if(cookies!=null){
			for(Cookie cookie:cookies){
				if(cookie.getName().equals("pids")){
					pids = cookie.getValue();
					//将pids拆成数组
					String[] split = pids.split("-");
					List<String> asList = Arrays.asList(split);
					LinkedList<String> list = new LinkedList<String>(asList);
					//判断集合中是否存在当前的pid
					if(list.contains(pid)){
						//包含当前商品
						list.remove(pid);
						list.addFirst(pid);
					}else{
						//不包含当前商品
						list.addFirst(pid);
					}
					//转成字符串
					StringBuffer sb = new StringBuffer();
					for(int i=0;i<list.size()&&i<7;i++){
						sb.append(list.get(i));
						sb.append("-");
					}
					//去掉最后一个“-”
					pids = sb.substring(0, sb.length()-1);
				}
			}
		}
		Cookie cookie_pids = new Cookie("pids",pids);
		response.addCookie(cookie_pids);
		
		request.getRequestDispatcher("product_info.jsp").forward(request,response);
	}

	
	
	//根据分类查询商品
	public void productListByCid(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		ProductService service = new ProductService();
		
		//获得Cid
		String cid = request.getParameter("cid");
		
		String currentPageStr = request.getParameter("currentPage");
		if(currentPageStr==null) currentPageStr="1";
		int currentPage = Integer.parseInt(currentPageStr);
		int currentCount = 12;
		
		PageBean pageBean = service.findProductListByCid(cid,currentPage,currentCount);
		
		request.setAttribute("pageBean", pageBean);
		request.setAttribute("cid", cid);
		
		//定义一个记录历史商品的集合
		List<Product> historyProductList = new ArrayList<Product>();
		//获得客户端携带名字叫pids的cookie
		Cookie[] cookies = request.getCookies();
		if(cookies!=null){
			for(Cookie cookie:cookies){
				if(cookie.getName().equals("pids")){
					String pids = cookie.getValue();
					String[] split = pids.split("-");
					for(String pid:split){
						Product pro = service.findProductByPid(pid);
						historyProductList.add(pro);
					}
				}
			}
		}
		//将历史纪录的集合放到域中
		request.setAttribute("historyProductList", historyProductList);
		request.getRequestDispatcher("/product_list.jsp").forward(request, response);
		
	}

}