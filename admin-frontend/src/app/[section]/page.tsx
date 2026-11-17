import { ManagementWorkspace } from "@/components/management-workspace";
export default async function AdminSectionPage({params}:{params:Promise<{section:string}>}){const {section}=await params;return <ManagementWorkspace section={section}/>;}
