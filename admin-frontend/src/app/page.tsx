import AdminShell from "@/components/admin-shell";
import { Activity, ArrowDownRight, ArrowUpRight, CreditCard, Package, ShoppingBag, Users } from "lucide-react";

const metrics = [
  { label: "Gross revenue", value: "৳ 2,48,500", change: "+12.8%", up: true, icon: CreditCard },
  { label: "Orders", value: "184", change: "+8.2%", up: true, icon: ShoppingBag },
  { label: "Customers", value: "1,240", change: "+4.6%", up: true, icon: Users },
  { label: "Products", value: "86", change: "-1.4%", up: false, icon: Package }
];
const orders = [
  ["#LP-1048", "Nusrat Jahan", "৳ 4,800", "Processing"],
  ["#LP-1047", "Tanvir Ahmed", "৳ 7,200", "Shipped"],
  ["#LP-1046", "Sadia Rahman", "৳ 3,600", "Pending"],
  ["#LP-1045", "Rafi Islam", "৳ 5,400", "Delivered"]
];
export default function DashboardPage() {
  return <AdminShell active="Overview" title="Dashboard" description="A clear view of your store performance.">
    <div className="metric-grid">{metrics.map(({label,value,change,up,icon:Icon})=><article className="metric-card" key={label}><div className="metric-top"><span>{label}</span><Icon size={18}/></div><strong>{value}</strong><small className={up?"positive":"negative"}>{up?<ArrowUpRight size={14}/>:<ArrowDownRight size={14}/>} {change} <em>vs last month</em></small></article>)}</div>
    <div className="dashboard-grid">
      <section className="panel chart-panel"><div className="panel-heading"><div><span className="eyebrow">PERFORMANCE</span><h2>Revenue overview</h2></div><select aria-label="Revenue period" defaultValue="7 days"><option>7 days</option><option>30 days</option><option>12 months</option></select></div><div className="chart"><div className="chart-y"><span>৳ 50k</span><span>৳ 35k</span><span>৳ 20k</span><span>৳ 5k</span></div><div className="chart-bars">{[38,58,46,76,52,88,66,95,61,79,48,72].map((h,i)=><div key={i} style={{height:h+"%"}}><i/></div>)}</div></div><div className="chart-x"><span>01 Nov</span><span>05 Nov</span><span>10 Nov</span><span>15 Nov</span><span>17 Nov</span></div><p className="demo-note">Illustrative demo analytics — not connected to live sales data.</p></section>
      <section className="panel activity-panel"><div className="panel-heading"><div><span className="eyebrow">STORE HEALTH</span><h2>Quick insights</h2></div><Activity size={18}/></div><div className="insight"><span>Low stock items</span><b>08</b></div><div className="insight"><span>Pending orders</span><b>12</b></div><div className="insight"><span>Unanswered reviews</span><b>06</b></div><div className="insight"><span>Newsletter subscribers</span><b>842</b></div><p className="demo-note">Sample figures for UI preview.</p></section>
    </div>
    <section className="panel orders-panel"><div className="panel-heading"><div><span className="eyebrow">LATEST ACTIVITY</span><h2>Recent orders</h2></div><a className="text-link" href="/orders">View all orders →</a></div><div className="table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Total</th><th>Status</th></tr></thead><tbody>{orders.map(o=><tr key={o[0]}><td className="order-id">{o[0]}</td><td>{o[1]}</td><td>{o[2]}</td><td><span className={"status "+o[3].toLowerCase()}>{o[3]}</span></td></tr>)}</tbody></table></div><p className="demo-note">All dashboard data is static demo content until the backend is connected.</p></section>
  </AdminShell>;
}