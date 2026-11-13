
LOOP
  fetch task
  if HARD --> plan, reset session, fetch plan
  implement
  test, verify
  commit


analyze logs and find places where model was not sure, got stuck, or went out of its way.
--> get new logs

make sure it has access to internet to fetch latest docs
