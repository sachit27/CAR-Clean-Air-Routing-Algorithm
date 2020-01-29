# CAR-Clean-Air-Routing-Algorithm

This repository contains the source code for CAR application. 

Abstract: Transport related pollution is becoming a major issue as it adversely affects human health and one way to lower the personal exposure to air pollutants is to choose a health-optimal route to the destination. Current navigation systems include options for the quickest paths (distance, traffic) and least expensive paths (fuel costs, tolls). In this paper, we come up with the CAR (Clean Air Routing) algorithm and use it to build a health-optimal route recommendation system between the origin and the destination. We combine the open source PM2.5 (Fine Particulate Matter with diameter less than 2.5 micrometers) concentration data for Taiwan, with the road network graph obtained through OpenStreetMaps. In addition, spatio-temporal interpolation of PM2.5 is performed to get PM2.5 concentration for the road network intersections. Our algorithm introduces a weight function that assesses how much PM2.5 the user is exposed to at each intersection of the road network and uses it to navigate through intersections with the lowest PM2.5 exposures. The algorithm can help people reduce their overall PM2.5 exposure by offering a healthier alternative route which may be slightly longer than the shortest path in some cases. We evaluate our algorithm for different travel modes, including driving, cycling and walking. An analysis is done for more than 4,000 real-world travel scenarios. The results show that our approach can lead to an average exposure reduction of 17.1% with an average distance increase of 2.4%.

To get more details about the algorithm, please have a look at our paper "CAR: The Clean Air Routing Algorithm for Path Navigation With Minimal PM2.5 Exposure on the Move" (https://ieeexplore.ieee.org/abstract/document/8863351). The paper is open access and available at the IEEE website. 
A demonstration of this work was also done at ACM MobiSys 2018 in Munich.


