/*
 * Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of i>clicker Sakai integrate.
 *
 * i>clicker Sakai integrate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * i>clicker Sakai integrate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.
 */
/*jslint undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, regexp: true, newcap: true, immed: true */
/*global jQuery, alert, window */
/**
 * This provides general utility methods which can be reused all over the app
 * 
 * @author azeckoski
 */
var Iclicker = Iclicker || {};
( function($) {
	Iclicker.initStatusChecker = function(statusSelector) {
		var statusHolder = $(statusSelector);
		$(".runner_button").attr("disabled", true); // disable all controls if this is running
		var timer = window.setInterval( function() {
			$.ajax( {
				url :"runnerStatus.jsp",
				dataType :"json",
				cache :false,
				success : function(status) {
					statusHolder.text(status.percent + "%");
					if (status.error) {
						statusHolder.text("ERROR");
						statusHolder.css('color', 'red');
						window.clearInterval(timer); // stop the timer
					} else if (status.complete) {
						statusHolder.css('color', 'blue');
						window.clearInterval(timer); // stop the timer
						$(".runner_button").removeAttr("disabled"); // enable the controls when complete
					} else {
						statusHolder.css('color', 'green');
					}
				},
				error : function(request, msg, errorThrown) {
					var status = request.status;
					window.clearInterval(timer); // stop the timer
					alert("Failure: " + status + " :" + msg + " :" + errorThrown);
				}
			});
		}, 3000);
	};
}(jQuery));
