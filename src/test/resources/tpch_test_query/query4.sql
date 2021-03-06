select
    o_orderpriority,
    count(*) as order_count
from
    orders_scrambled join lineitem_scrambled
        on l_orderkey = o_orderkey
where
    o_orderdate >= date '1992-12-01'
    and o_orderdate < date '1998-12-01'
    and l_commitdate < l_receiptdate
group by
    o_orderpriority
order by
    o_orderpriority