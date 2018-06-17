<html>
	<head>
		<link href="static/main.css" media="all" rel="stylesheet" type="text/css"/>
		<script src="static/jquery-1.8.3.min.js"></script>
		<script src="static/jquery.elevatezoom.js"></script>
		<%@ page import="java.util.Enumeration" %>
		<%@ page import="com.github.saschawiegleb.ek.entity.Ad" %>
		<%@ page import="java.lang.System" %>
		<%@ page import="java.util.*" %>
		<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
		<title>shAdOwl - owl ad shadower</title>
	</head>
	<%@ page session="true"%>
	<body id="srchrslt" class="">
		<jsp:useBean id='adsearch' scope='session' class='com.github.saschawiegleb.ek.watcher.lucene.AdSearch' type="com.github.saschawiegleb.ek.watcher.lucene.AdSearch" />
    	<div id="site-content" class="l-page-wrapper l-container-row  " style="visibility: visible;">
        	<div class="l-splitpage">
        	<img src="static/logo.jpg" alt="Owl-shADow" width="75%">
        		<form action = "index.jsp" method = "GET" style="float: center">
					Search: <input type = "text" name = "search_string" value="<%= request.getParameter("search_string") %>">
					<input type = "submit" value = "Submit" />
				</form>
            	<div id="srchrslt-content" class="l-splitpage-content position-relative">
                	<div class="l-container-row contentbox-unpadded">
                    	<div class="position-relative">
							<ul id="srchrslt-adtable" class="itemlist-separatedbefore ad-list">
								<c:forEach var="item" items="<%= adsearch.luceneQuery(request.getParameter(\"search_string\")) %>" varStatus="status">
									<% Ad ad = (Ad)pageContext.getAttribute("item");%>			
									<li class="ad-listitem">
										<article data-adid="<%= ad.id()%>">
											<section class="aditem-image">
                								<div class="imagebox srpimagebox" data-href="https://www.ebay-kleinanzeigen.de/s-anzeige/<%= ad.id()%>" data-imgsrc="<%= !ad.images().isEmpty()?ad.images().get(0):"https://images-eu.ssl-images-amazon.com/images/I/21ZCh1dT-8L.jpg"%>" data-imgtitle="<%= ad.headline()%> Vorschau" style="cursor: pointer;">
                                					<img id="img_<%= ad.id()%>" class="lazy" src="<%= !ad.images().isEmpty()?ad.images().get(0).replace("57.JPG", "9.JPG"):"https://images-eu.ssl-images-amazon.com/images/I/21ZCh1dT-8L.jpg"%>" data-zoom-image="<%= !ad.images().isEmpty()?ad.images().get(0):"https://images-eu.ssl-images-amazon.com/images/I/21ZCh1dT-8L.jpg"%>" height="150">
                                				</div>
                            				</section>
            								<section class="aditem-main">
                								<h2 class="text-module-begin"><a href="https://www.ebay-kleinanzeigen.de/s-anzeige/<%= ad.id()%>"><%= ad.headline()%></a></h2>
                								<p><%= ad.description().substring(0,ad.description().length()<100?ad.description().length():100).concat("...")%></p>
                							</section>
            								<section class="aditem-details">
                								<strong><%= ad.price()%></strong><br>
                								<%= ad.location()%><br>
                							</section>
            								<section class="aditem-addon">
                								<%= ad.time().get()%>
                							</section>
        								</article>
        								<script>
    										$("#img_<%= ad.id()%>").elevateZoom({scrollZoom : true});
										</script>
    								</li>
								</c:forEach>
							</ul>
                    	</div>
                	</div>
            	</div>
        	</div>
    	</div>

    	<script src="https://cdnjs.cloudflare.com/ajax/libs/vanilla-lazyload/8.5.2/lazyload.min.js"></script>
		<script>
	(function () {
		function logElementEvent(eventName, element) {
			console.log(new Date().getTime(), eventName, element.getAttribute('data-src'));
		}
		function logEvent(eventName, elementsLeft) {
			console.log(new Date().getTime(), eventName, elementsLeft + " images left");
		}
		/* Uncomment the callbacks in LazyLoad options to see the callbacks logs in your browser's console */
		new LazyLoad({
			elements_selector: ".lazy"
			/*,
			callback_load: function (element) {
				logElementEvent("LOADED", element);
			},
			callback_set: function (element) {
				logElementEvent("SET", element);
			},
			callback_processed: function(elementsLeft) {
				logEvent("PROCESSED", elementsLeft);
			}*/
		});
	}());
</script>
	</body>
</html>
