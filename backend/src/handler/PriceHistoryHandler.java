package handler;

import app.Database;
import app.SessionManager;
import app.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.*;
import java.util.*;

public class PriceHistoryHandler implements HttpHandler {
  public void handle(HttpExchange ex) throws IOException {
    if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { JsonResponse.send(ex,405,JsonResponse.error("GET method required.")); return; }
    if (SessionManager.getUserId(ex.getRequestHeaders().getFirst("X-Session-Token")) == null) { JsonResponse.send(ex,401,JsonResponse.error("Please log in first.")); return; }
    Map<String,String> q=new HashMap<>(); String raw=ex.getRequestURI().getRawQuery();
    if(raw!=null) for(String pair:raw.split("&")){String[] a=pair.split("=",2);q.put(URLDecoder.decode(a[0],"UTF-8"),a.length>1?URLDecoder.decode(a[1],"UTF-8"):"");}
    try {
      int productId=Integer.parseInt(q.getOrDefault("productId","0")); String sellerId=q.getOrDefault("sellerId","");
      if(productId<1){JsonResponse.send(ex,400,JsonResponse.error("A valid productId is required."));return;}
      String sql="SELECT ph.Price,ph.Discount,ph.DeliveryCharge,ph.PlatformFee,ph.RecordedAt,s.SellerID,s.SellerName " +
        "FROM PriceHistory ph JOIN SellerProducts sp ON ph.SellerProductID=sp.SellerProductID JOIN Sellers s ON sp.SellerID=s.SellerID WHERE sp.ProductID=? " +
        (sellerId.isEmpty()?"":"AND s.SellerID=? ")+"ORDER BY ph.RecordedAt ASC";
      List<String> records=new ArrayList<>(); Map<Integer,String> sellers=new LinkedHashMap<>();
      try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
        ps.setInt(1,productId); if(!sellerId.isEmpty()) ps.setInt(2,Integer.parseInt(sellerId));
        try(ResultSet rs=ps.executeQuery()){while(rs.next()){
          int sid=rs.getInt("SellerID"); sellers.put(sid,rs.getString("SellerName"));
          java.math.BigDecimal price=rs.getBigDecimal("Price"), disc=rs.getBigDecimal("Discount"), del=rs.getBigDecimal("DeliveryCharge"), fee=rs.getBigDecimal("PlatformFee");
          java.math.BigDecimal finalCost=price.subtract(disc).max(java.math.BigDecimal.ZERO).add(del).add(fee);
          double pct=price.signum()==0?0:disc.multiply(new java.math.BigDecimal("100")).divide(price,2,java.math.RoundingMode.HALF_UP).doubleValue();
          records.add("{\"recordedAt\":"+JsonUtil.quote(rs.getTimestamp("RecordedAt").toString())+",\"sellerId\":"+sid+",\"sellerName\":"+JsonUtil.quote(rs.getString("SellerName"))+",\"price\":"+price.toPlainString()+",\"discountPercent\":"+pct+",\"discountAmount\":"+disc.toPlainString()+",\"deliveryCharge\":"+del.toPlainString()+",\"platformFee\":"+fee.toPlainString()+",\"finalCost\":"+finalCost.toPlainString()+"}");
        }}
      }
      StringBuilder sj=new StringBuilder("["); for(Map.Entry<Integer,String> e:sellers.entrySet()){if(sj.length()>1)sj.append(',');sj.append("{\"sellerId\":").append(e.getKey()).append(",\"sellerName\":").append(JsonUtil.quote(e.getValue())).append('}');} sj.append(']');
      JsonResponse.send(ex,200,"{\"sellers\":"+sj+",\"records\":["+String.join(",",records)+"]}");
    } catch(NumberFormatException e){JsonResponse.send(ex,400,JsonResponse.error("Invalid product or seller ID."));}
      catch(SQLException e){JsonResponse.send(ex,500,JsonResponse.error("Could not load price history. Check the database connection."));}
  }
}
