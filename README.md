# Wirasat
Created initial schema

Added more tables

Created separate beneficiaries table to keep track of non-members beneficiaries

Created a separate table to keep track of how many times a certain individual's assets have been distributed and calculated

run_id → calculation_runs.deceased_id → distribution_logs.deceased_id
removed deceased_id from distribution_logs

removed allocated_value from asset_allocations as it is computable

beneficiary types were repeating in beneficiaries table so added beneficiary_type

condition type repeating in share_rules table so added conditions_type
