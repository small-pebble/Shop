package com.itheima.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.itheima.dao.ProductDao;
import com.itheima.domain.Category;
import com.itheima.domain.Order;
import com.itheima.domain.OrderItem;
import com.itheima.domain.PageBean;
import com.itheima.domain.Product;
import com.itheima.utils.DataSourceUtils;

public class ProductService {
	
	ProductDao dao = new ProductDao();

	//获得热门商品
	public List<Product> findHotProductList() {
		List<Product> hotProductList = null;
		try {
			hotProductList = dao.findHotProductList();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return hotProductList;
	}

	//获得最新商品
	public List<Product> findNewProductList() {
		List<Product> newProductList = null;
		try {
			newProductList = dao.findNewProductList();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return newProductList;
	}

	public List<Category> findAllCategory() {
		List<Category> categoryList = null;
		try {
			categoryList = dao.findAllCategory();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return categoryList;
	}

	public PageBean findProductListByCid(String cid,int currentPage,int currentCount) {
		//封装一个PageBean返回给web层
		PageBean<Product> pageBean = new PageBean<Product>();
		
		//1.封装当前页
		pageBean.setCurrentPage(currentPage);
		//2.封装每页显示的条数
		pageBean.setCurrentCount(currentCount);
		//3.封装总条数
		int totalCount = 0;
		try {
			totalCount = dao.getCount(cid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		pageBean.setTotalCount(totalCount);
		//4.封装总页数
		int totalPage = (int) Math.ceil(1.0*totalCount/currentCount);
		pageBean.setTotalPage(totalPage);
		//5.当前页显示的数据
		int index = (currentPage-1)*currentCount;
		List<Product> list = null;
		try {
			list = dao.findProductByPage(cid,index,currentCount);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		pageBean.setList(list);
		
		
		return pageBean;
	}

	public Product findProductByPid(String pid) {
		Product product = null;
		try {
			product = dao.findProductByPid(pid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return product;
	}

	
	//提交订单。将订单和订单项存到数据库中
	public void submitOrder(Order order) {
		ProductDao dao = new ProductDao();

		try {
			//1.开启事务
			DataSourceUtils.startTransaction();
			//2.调用dao存储order表数据的方法
			dao.addOrders(order);
			//3.调用dao存储数据项的方法
			dao.addOrderItem(order);
		} catch (SQLException e) {
			try {
				DataSourceUtils.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}finally{
			try {
				DataSourceUtils.commitAndRelease();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateOrderState(String r6_Order) {
		try {
			dao.updateOrderState(r6_Order);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateOrderAddr(Order order) {
		ProductDao dao = new ProductDao();
		try {
			dao.updateOrderAddr(order);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	//获得该用户的所有订单
	public List<Order> findAllOrdersByUid(String uid) {
		List<Order> orderList = null;
		try {
			orderList = dao.findAllOrdersByUid(uid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return orderList;
	}

	//用订单项填充订单信息
	public List<Map<String,Object>> findAllOrderItemsByOid(String oid) {
		List<Map<String,Object>> mapList = null;
		try {
			mapList = dao.findAllOrderItemsByUid(oid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return mapList;
	}



}
