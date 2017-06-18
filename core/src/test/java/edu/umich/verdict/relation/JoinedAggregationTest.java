package edu.umich.verdict.relation;

import edu.umich.verdict.VerdictConf;
import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.exceptions.VerdictException;

public class JoinedAggregationTest {

	public static void main(String[] args) throws VerdictException {
		VerdictConf conf = new VerdictConf();
		conf.setDbms("impala");
		conf.setHost("salat1.eecs.umich.edu");
		conf.setPort("21050");
		conf.setDbmsSchema("instacart1g");
		conf.set("no_user_password", "true");
		VerdictContext vc = new VerdictContext(conf);
		
		SingleRelation r1 = SingleRelation.from(vc, "order_products");
		SingleRelation r2 = SingleRelation.from(vc, "orders");
		SingleRelation r3 = SingleRelation.from(vc, "products");
		System.out.println(
				r1.join(r2).where("order_products.order_id = orders.order_id")
				.join(r3).where("order_products.product_id = products.product_id")
				.groupby("order_dow")
				.approxCountDistincts("orders.user_id")
				.orderby("order_dow")
				.collectAsString());
//		System.out.println(
//				r1.join(r2).where("order_products.order_id = orders.order_id")
//				.groupby("order_dow").approxCounts()
//				.collectAsString());
//		System.out.println(r1.join(r2).where("order_products.order_id = orders.order_id").approxCountDistinct("user_id"));
//		System.out.println(r1.join(r2).where("order_products.order_id = orders.order_id").count());
		
		
		vc.destroy();
	}

}
