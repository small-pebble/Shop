package com.itheima.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.itheima.domain.Category;
import com.itheima.domain.Order;
import com.itheima.domain.OrderItem;
import com.itheima.domain.Product;
import com.itheima.utils.DataSourceUtils;

public class ProductDao {
	
	QueryRunner runner = new QueryRunner(DataSourceUtils.getDataSource());

	//获得热门商品
	public List<Product> findHotProductList() throws SQLException {
		String sql = "select * from product where is_hot=? limit ?,?";
		return runner.query(sql,new BeanListHandler<Product>(Product.class),1,0,9);
	}

	//获得最新商品
	public List<Product> findNewProductList() throws SQLException {
		String sql = "select * from product order by pdate desc limit ?,?";
		return runner.query(sql, new BeanListHandler<Product>(Product.class), 0,9);
	}

	public List<Category> findAllCategory() throws SQLException {
		String sql = "select * from category";
		List<Category> CategoryList = runner.query(sql, new BeanListHandler<Category>(Category.class));
		return CategoryList;
	}

	public int getCount(String cid) throws SQLException {
		String sql = "select count(*) from product where cid=?";
		Long query = (Long) runner.query(sql, new ScalarHandler(),cid);
		return query.intValue();
	}

	public List<Product> findProductByPage(String cid, int index, int currentCount) throws SQLException {
		String sql = "select * from product where cid=? limit ?,?";
		return runner.query(sql, new BeanListHandler<Product>(Product.class),cid,index,currentCount);
	}

	public Product findProductByPid(String pid) throws SQLException {
		String sql = "select * from product where pid=?";
		return runner.query(sql, new BeanHandler<Product>(Product.class),pid);
	}

	
	//向order表插入数据
	public void addOrders(Order order) throws SQLException {
		QueryRunner runner = new QueryRunner();
		String sql = "insert into orders values(?,?,?,?,?,?,?,?)";
		Connection conn = DataSourceUtils.getConnection();
		runner.update(conn, sql, order.getOid(), order.getOrdertime(),order.getTotal(),order.getState(),
				order.getAddress(),order.getName(),order.getTelephone(),order.getUser().getUid());
	}

	//向orderItem表插入数据
	public void addOrderItem(Order order) throws SQLException {
		QueryRunner runner = new QueryRunner();
		String sql = "insert orderitem values(?,?,?,?,?)";
		Connection conn = DataSourceUtils.getConnection();
		List<OrderItem> orderItems = order.getOrderItems();
		for(OrderItem item:orderItems){
			runner.update(conn, sql, item.getItemid(),item.getCount(),item.getSubtotal(),item.getProduct().getPid(),item.getOrder().getOid());
		}
	}
	
	//提交订单
	public void updateOrderAddr(Order order) throws SQLException {
		String sql = "update orders set address=?,name=?,telephone=? where oid=?";
		runner.update(sql, order.getAddress(),order.getName(),order.getTelephone(),order.getOid());
	}


	//修改订单状态
	public void updateOrderState(String r6_Order) throws SQLException {
		String sql = "update orders set state=? where oid=?";
		runner.update(sql, 1,r6_Order);
	}

	//单表查询用户的订单
	public List<Order> findAllOrdersByUid(String uid) throws SQLException {
		String sql = "select * from orders where uid=?";
		return runner.query(sql, new BeanListHandler<Order>(Order.class), uid);
	}

	public List<Map<String,Object>> findAllOrderItemsByUid(String oid) throws SQLException {
		String sql = "select i.count,i.subtotal,p.pimage,p.pname,p.shop_price from orderitem i,product p where i.pid=p.pid and i.oid=?";
		List<Map<String,Object>> mapList = runner.query(sql, new MapListHandler(), oid);
		return mapList;
	}

}
