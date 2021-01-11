/**
* A set level of UL plugin for jQuery
* @author: Mover Zhou 
* @url http://ingod.asia
* Version: 1.0
* Updated: November 9th, 2011
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
*/

(function($){
	$.fn.setLevel=function(i){
		$this = $(this);
		$this.data("level",i+1);
		$(this).removeClass("level-"+i);
		$(this).addClass("level-"+$this.data("level"));
		
		$this.children("li").each(function(){
			$(this).children("ul").each(function(){
				$(this).setLevel($(this).data("level"));
			});
		});
		
		return this;
	}
	
	$.fn.level=function(){
		return $(this).data("level");;
	}
})(jQuery);;