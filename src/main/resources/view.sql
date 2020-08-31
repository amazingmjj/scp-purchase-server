--采购计划主表视图
create or replace view  v_t_purchase_plan as
select t.*,c.name as company_name,e.name as employee_name,o.name as org_name,d.name as dpt_name
from t_purchase_plan t
left join t_company c on t.company_code=c.code
left join t_employee e on t.employee_code=e.code
left join t_org o on t.org_code=o.code
left join t_dpt d on t.dpt_code=d.code;