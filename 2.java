package com.offcn.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.cart.service.CartService;
import com.offcn.group.Cart;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
//**************************************服务器端修改****************************************
@Service
public class CartServiceImpl implements CartService {

    //注入sku表数据操作接口
    @Autowired
    private TbItemMapper itemMapper;

    //注入Redis模板操作工具类
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
       //1、跟据sku商品编号，查询sku商品信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        //1.1、判断sku对象是否为空
        if(item==null){
            //商品不存在，结束添加购物车操作，抛出异常
            throw new RuntimeException("要添加到购物车的商品不存在");
        }
        //1.2、判断商品状态 是否是审核通过 1
        if(!item.getStatus().equals("1")){
            throw new RuntimeException("商品未审核通过");
        }

        //2、获取该商品所属商家编号
        String sellerId = item.getSellerId();
        //3、根据商家编号，去遍历购物车集合，判断是否存在该商家购物车对象
        Cart cart = searchCartListBySellerId(cartList, sellerId);
        //4、判断该商家的购物车对象是否存在
        if(cart==null){
            //5、第一个情况，该商家购物车对象不存在，创建该商家的购物车对象
            cart=new Cart();
            //设置购物车对象，所属商家编号
            cart.setSellerId(sellerId);
            //设置购物车对象，商家名称
            cart.setSellerName(item.getSeller());
            //创建一个购物明细集合
            List<TbOrderItem> orderItemList=new ArrayList<>();
            //创建购物明细
            TbOrderItem orderItem = createOrderItem(item, num);
            //把购物明细存放到购物明细集合
            orderItemList.add(orderItem);
            //把购物明细集合添加到购物车对象
            cart.setOrderItemList(orderItemList);

            //千万别忘记，把新建的购物车对象添加到当前购物车集合
            cartList.add(cart);


        }else {
            //6、第二种情况，购物车集合存在该商家购物车对象

            //7、需要判断要加入到购物车商品在该商家购物车里面的购物明细中是否存在
            TbOrderItem orderItem = searchOrderItemByItemid(cart.getOrderItemList(), itemId);
             //8、第一种情况，要添加到购物车商品在购物明细中目前不存在
            if(orderItem==null){
                //创建一个购物明细对象，
                orderItem= createOrderItem(item,num);
                //把购物明细增加当前购物对象的购物明细集合
                cart.getOrderItemList().add(orderItem);
            }else {
                //9、第二种情况，要添加的商品在购物车对象的购物明细中已经存在
                //更新购买数量 原有购买数量+新的购买数量
                orderItem.setNum(orderItem.getNum()+num);
                //更新当前商品合计金额
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum()*item.getPrice().doubleValue()));

                //调整购物车购买数量完成后，需要判断当前购买数量是否小于1，如果当前商品购买数量小于1，要要当前购物车
                //购物明细集合移除该商品
                if(orderItem.getNum()<1){
                    cart.getOrderItemList().remove(orderItem);
                }

                //当购物车的购物明细集合为0，从购物车集合移除当前商家购物车对象
                if(cart.getOrderItemList().size()==0){
                    cartList.remove(cart);
                }
            }

        }

        return cartList;
    }



    /**
     * 遍历购物车集合，判断是否存在该商家购物车对象
     * @param cartList  要搜索的购物车集合
     * @param sellerId   要比对的商家编号
     * @return   搜索到的该商家的购物车对象
     */
    private Cart searchCartListBySellerId(List<Cart> cartList,String sellerId){

        //判断购物车集合是否为空
        if(cartList!=null){
            //遍历购物车集合
            for (Cart cart : cartList) {
                //提取购物车对象的商家属性，和传入的商家编号进行比对
                if(cart.getSellerId().equals(sellerId)){
                    //返回当前购物车对象
                    return cart;
                }
            }
        }

        return null;

    }



    /**
     * 创建购物明细
     * @param item  要购买的商品sku对象
     * @param num   要购买的数量
     * @return   购物明细对象
     */
    private TbOrderItem createOrderItem(TbItem item,Integer num){

        //判断购买数量是否小于1，购买数量不合法

        if(num<1){
            throw new RuntimeException("购买数量不合法");
        }
        //new购物明细对象
        TbOrderItem orderItem = new TbOrderItem();

        //设置购物明细属性
        //设置商品编号
        orderItem.setGoodsId(item.getGoodsId());
        //设置sku编号
        orderItem.setItemId(item.getId());
        //设置购买数量
        orderItem.setNum(num);
        //图片
        orderItem.setPicPath(item.getImage());
        //价格
        orderItem.setPrice(item.getPrice());
        //商家编号
        orderItem.setSellerId(item.getSellerId());
        //商品标题
        orderItem.setTitle(item.getTitle());
        //设置单个商品合计费用  = 购买数量*商品单价
        //创建购买数量高精度
        BigDecimal bigDecimalNum = new BigDecimal(num);

        orderItem.setTotalFee(bigDecimalNum.multiply(item.getPrice()));

        return orderItem;
    }



    /**
     * //判断指定商家购物车对象，是否存在指定购物明细
     * @param orderItemList
     * @param itemId
     * @return
     */
    private TbOrderItem searchOrderItemByItemid(List<TbOrderItem> orderItemList,Long itemId){
        //遍历购物明细
        for (TbOrderItem orderItem : orderItemList) {
            //提取购物明细对象，商品sku编号，和传递过来的商品sku编号比对，看是否一致
            if(orderItem.getItemId().longValue()==itemId.longValue()){
                return orderItem;
            }
        }

        return null;
    }

    @Override
    public List<Cart> findCartListFromRedis(String username) {

        List<Cart> cartList=null;
        cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);


        if(cartList==null){
          cartList=  new ArrayList<>();
        }

        return cartList;

    }

    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        System.out.println("存入购物车数据:"+username);
        redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    @Override
    public List<Cart> mergeCartList(List<Cart> cookieCartlist, List<Cart> redisCartList) {
        //遍历cookie购物车集合
        for (Cart cart : cookieCartlist) {
            //循环遍历购物明细集合
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                //调用添加到购物车方法
              redisCartList=  addGoodsToCartList(redisCartList,orderItem.getItemId(),orderItem.getNum());
            }
        }

        return redisCartList;
    }
}
