package dao;
import app.Database; import java.sql.*; import java.util.*;
public class RecommendationDao {
 public List<Map<String,Object>> getRecommendations(int userId)throws SQLException{
  List<Map<String,Object>> out=new ArrayList<>();
  String sql="SELECT RecommendationID,RecommendationType,CreatedAt FROM Recommendations WHERE UserID=? ORDER BY RecommendationID DESC";
  try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("recommendationId",r.getInt("RecommendationID"));m.put("type",r.getString("RecommendationType"));m.put("createdAt",r.getTimestamp("CreatedAt").toString());List<Map<String,Object>> items=new ArrayList<>();String q="SELECT p.Name,s.SellerName,ri.RecommendedPrice FROM RecommendationItems ri JOIN Products p ON p.ProductID=ri.ProductID LEFT JOIN SellerProducts sp ON sp.SellerProductID=ri.SellerProductID LEFT JOIN Sellers s ON s.SellerID=sp.SellerID WHERE ri.RecommendationID=?";try(PreparedStatement ip=c.prepareStatement(q)){ip.setInt(1,r.getInt("RecommendationID"));try(ResultSet ir=ip.executeQuery()){double total=0;while(ir.next()){Map<String,Object> item=new LinkedHashMap<>();item.put("productName",ir.getString("Name"));item.put("sellerName",ir.getString("SellerName")==null?"Seller unavailable":ir.getString("SellerName"));item.put("recommendedPrice",ir.getBigDecimal("RecommendedPrice"));total+=ir.getBigDecimal("RecommendedPrice").doubleValue();items.add(item);}m.put("itemsTotal",total);}}m.put("items",items);out.add(m);}}}return out;
 }
}
