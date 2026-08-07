import { Link } from "react-router-dom";
import type { MembershipPackage } from "../types/membershipPackage.types";
const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });
export function PackagePreview({ item }: { item: MembershipPackage }) { return <article className="landing-package"><div><span>{item.durationDays} days</span><h3>{item.name}</h3></div><strong>{money.format(item.price)}</strong><p>{item.description}</p><Link to="/register">Choose package</Link></article>; }
