dataChecks2.m

Most of the checks in the this scripts only apply when latency is zero.

The first thing we check is that the number of headers matches the numbers of columns in the data. If these don't match, that implies that something went wrong when parsing the file.

The second thing we check is the range of the latency, which should be between 0 and 1000. If latency is not the variable being varied it will always be 0.

Next, we check that various things match at latency zero. Undiscounted surplus should match, as well as transaction number, execution speeds, and spreads at med nbbo. We check that discounted surplus matches at every market except central call.

Last we check that various volatilities match. For average volatility, we check that central call and central cda match and twomarket dummy and two market la match. When markets are involved we check that 1 and 6, 2 and 4, and 3 and 5 match.
