webpackJsonp([1],[,,function(t,e,i){"use strict";i.d(e,"a",function(){return n});var a=i(1),n=new a.a},,,,,,function(t,e,i){"use strict";var a=i(1);e.a={getMovies:function(t,e,i){var n="/api/movies/?page="+e;return null!==t&&""!==t&&(n="/api/movies/search/by-name?name="+t+"&page="+e),a.a.http.get(n).then(function(t){var e=t.data;i&&i(!0,e)},function(t){var e=t.data;i&&i(!1,e)})},getMovie:function(t,e){return a.a.http.get("/api/movies/"+t).then(function(t){var i=t.data;e&&e(!0,i)},function(t){var i=t.data;e&&e(!1,i)})}}},,,,function(t,e,i){"use strict";function a(t,e,i){if(!t)return"";if(i=i||"...",e=e||300,t.length<=e)return t;for(var a=t.slice(0,e-i.length),n=a.length-1;n>0&&" "!==a[n]&&a[n]!==i[0];)n-=1;return n=n||e-i.length,(a=a.slice(0,n))+i}function n(t){var e=new Date(t),i=e.getUTCMonth()+1,a=e.getUTCDate();return e.getUTCFullYear()+"-"+i+"-"+a}function s(t,e){return t?(e=e||" / ",t.map(function(t){return t.name||t}).join(e)):""}function o(t){return t.replace("http://www.imdb.com/title/","")}Object.defineProperty(e,"__esModule",{value:!0}),e.truncate=a,e.date=n,e.join=s,e.imdb=o},function(t,e,i){"use strict";var a=i(1),n=i(69),s=i(59),o=i.n(s),r=i(58),c=i.n(r);a.a.use(n.a),e.a=new n.a({routes:[{path:"/",name:"MovieListView",component:o.a},{path:"/movies/:id",name:"MovieDetail",component:c.a}]})},,function(t,e){},,function(t,e,i){function a(t){i(55)}var n=i(0)(i(21),i(65),a,null,null);t.exports=n.exports},,function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var a=i(14),n=i.n(a),s=i(1),o=i(18),r=i(17),c=i.n(r),l=i(13),d=i(12);i(15),i(16),s.a.config.productionTip=!1,s.a.use(o.a),n()(d).forEach(function(t){s.a.filter(t,d[t])}),new s.a({el:"#app",router:l.a,template:"<App/>",components:{App:c.a}})},function(t,e,i){"use strict";e.a={data:{},setItem:function(t,e){"undefined"!=typeof Storage?localStorage.setItem(t,e):this.data[t]=e},getItem:function(t){return"undefined"!=typeof Storage?localStorage.getItem(t):this.data[t]}}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0}),e.default={name:"app"}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var a=i(8);e.default={name:"MovieDetail",data:function(){return{loading:!1,error:"",movie:null}},created:function(){this.fetchData()},watch:{$route:"fetchData"},methods:{fetchData:function(){var t=this;this.error=this.movie=null,this.loading=!0,a.a.getMovie(this.$route.params.id,function(e,i){t.loading=!1,e?t.movie=i:t.error=i.message||"Bad Request"})},isNotEmpty:function(t){return void 0!==t&&null!==t&&null!==t.length&&t.length>0}}}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var a=i(8),n=i(20),s=i(60),o=i.n(s),r=i(61),c=i.n(r),l=i(2);e.default={name:"MovieListView",components:{VuePagination:o.a,VuePaginationInfo:c.a},data:function(){return{loading:!1,error:"",text:this.$route.query.search||n.a.getItem("search")||"",currentPage:this.$route.query.page||n.a.getItem("currentPage")||0,pagination:null,movies:[]}},created:function(){this.loadData()},methods:{loadData:function(){var t=this;this.error=this.movies=null,this.loading=!0,n.a.setItem("search",this.text),n.a.setItem("currentPage",this.currentPage),a.a.getMovies(this.text,this.currentPage,function(e,i){t.loading=!1,e?(t.fireEvent("load-success",i),t.movies=i._embedded.movies,t.pagination=t.getPaginationData(i.page),t.$nextTick(function(){this.fireEvent("pagination-data",this.pagination),this.fireEvent("loaded")})):(t.error=i.message||"Bad Request",t.fireEvent("load-error",i),t.fireEvent("loaded"))})},search:function(){this.currentPage=0,this.loadData()},getPaginationData:function(t){var e=t.numberOfElements||t.size;return t.from=t.number*t.size+1,t.to=t.from+e-1,t.to>t.totalElements&&(t.to=t.totalElements),t},fireEvent:function(t,e){l.a.$emit("vue-pagination:"+t,e)},changePage:function(t){"prev"===t?this.gotoPreviousPage():"next"===t?this.gotoNextPage():this.gotoPage(t)},gotoPreviousPage:function(){this.currentPage>0&&(this.currentPage--,this.loadData())},gotoNextPage:function(){this.currentPage+1<this.pagination.totalPages&&(this.currentPage++,this.loadData())},gotoPage:function(t){t!==this.currentPage&&t>=0&&t<this.pagination.totalPages&&(this.currentPage=t,this.loadData())}}}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var a=i(63),n=i.n(a);e.default={mixins:[n.a]}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var a=i(62),n=i.n(a),s=i(2);e.default={mixins:[n.a],computed:{paginationInfo:function(){return null===this.pagination||0===this.pagination.totalElements?this.noDataTemplate:this.infoTemplate.replace("{from}",this.pagination.from||0).replace("{to}",this.pagination.to||0).replace("{total}",this.pagination.totalElements||0)}},data:function(){return{pagination:null}},created:function(){this.registerEvents()},methods:{setPaginationData:function(t){this.pagination=t},registerEvents:function(){var t=this;s.a.$on("vue-pagination:pagination-data",function(e){t.setPaginationData(e)})}}}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0}),e.default={props:{infoClass:{type:String,default:function(){return"left floated left aligned five wide column"}},infoTemplate:{type:String,default:function(){return"Displaying {from} to {to} of {total} items."}},noDataTemplate:{type:String,default:function(){return"No relevant data."}}}}},function(t,e,i){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var a=i(2);e.default={props:{css:{type:Object,default:function(){return{wrapperClass:"ui right floated pagination menu",activeClass:"active large",disabledClass:"disabled",pageClass:"item",linkClass:"icon item",paginationClass:"ui bottom attached segment grid",paginationInfoClass:"left floated left aligned five wide column"}}},icons:{type:Object,default:function(){return{first:"angle double left icon",prev:"left chevron icon",next:"right chevron icon",last:"angle double right icon"}}},onEachSide:{type:Number,default:function(){return 2}}},data:function(){return{pagination:null}},created:function(){this.registerEvents()},computed:{totalPage:function(){return null===this.pagination?0:this.pagination.totalPages},isOnFirstPage:function(){return null!==this.pagination&&0===this.pagination.number},isOnLastPage:function(){return null!==this.pagination&&this.pagination.number+1===this.pagination.totalPages},notEnoughPages:function(){return this.totalPage<2*this.onEachSide+4},windowSize:function(){return 2*this.onEachSide+1},windowStart:function(){return!this.pagination||this.pagination.number<=this.onEachSide?0:this.pagination.number>=this.totalPage-this.onEachSide?this.totalPage-1-2*this.onEachSide:this.pagination.number-this.onEachSide}},methods:{loadPage:function(t){this.$emit("vue-pagination:change-page",t)},isCurrentPage:function(t){return t===this.pagination.number},setPaginationData:function(t){this.pagination=t},registerEvents:function(){var t=this;a.a.$on("vue-pagination:pagination-data",function(e){t.setPaginationData(e)})}}}},,,,,,,,,,,,,,,,,,,,,,,,,,,function(t,e){},function(t,e){},function(t,e){},,function(t,e,i){function a(t){i(56)}var n=i(0)(i(22),i(67),a,null,null);t.exports=n.exports},function(t,e,i){function a(t){i(54)}var n=i(0)(i(23),i(64),a,null,null);t.exports=n.exports},function(t,e,i){var a=i(0)(i(24),i(68),null,null,null);t.exports=a.exports},function(t,e,i){var a=i(0)(i(25),i(66),null,null,null);t.exports=a.exports},function(t,e,i){var a=i(0)(i(26),null,null,null,null);t.exports=a.exports},function(t,e,i){var a=i(0)(i(27),null,null,null,null);t.exports=a.exports},function(t,e){t.exports={render:function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("div",{staticClass:"ui container divided items",attrs:{id:"movies"}},[i("div",{staticClass:"ui hidden divider"}),t._v(" "),t.loading?i("div",{staticClass:"ui active dimmer"},[i("div",{staticClass:"ui loader"})]):t._e(),t._v(" "),i("div",{directives:[{name:"show",rawName:"v-show",value:t.error,expression:"error"}],staticClass:"ui error message",attrs:{transition:"fade"}},[t._v("\n    "+t._s(t.error)+"\n  ")]),t._v(" "),i("div",{staticClass:"vue-pagination ui basic segment grid"},[i("vue-pagination-info",{ref:"paginationInfo"}),t._v(" "),i("div",{staticClass:"ui input"},[i("input",{directives:[{name:"model",rawName:"v-model",value:t.text,expression:"text"}],attrs:{id:"search",type:"text",placeholder:"search by name"},domProps:{value:t.text},on:{keyup:function(e){if(!("button"in e)&&t._k(e.keyCode,"enter",13))return null;t.search(e)},input:function(e){e.target.composing||(t.text=e.target.value)}}})]),t._v(" "),i("vue-pagination",{ref:"pagination",on:{"vue-pagination:change-page":t.changePage}})],1),t._v(" "),t._l(t.movies,function(e){return i("div",{staticClass:"item movie",staticStyle:{"min-height":"225px"}},[i("router-link",{staticClass:"ui small image",attrs:{to:"/movies/"+e.id}},[i("img",{attrs:{src:e.thumb}})]),t._v(" "),i("div",{staticClass:"content"},[i("router-link",{staticClass:"header",attrs:{to:"/movies/"+e.id}},[t._v("\n        "+t._s(e.title)+"\n      ")]),t._v(" "),i("div",{staticClass:"description"},[i("p",[t._v(t._s(e.synopsis||"暂无介绍"))])]),t._v(" "),i("div",{staticClass:"extra"},[i("div",[i("span",{staticClass:"date"},[t._v(t._s(t._f("date")(e.createdTime)))]),t._v(" "),i("span",{staticClass:"category"},[t._v(t._s(t._f("join")(e.categories)))]),t._v(" "),i("a",{staticClass:"imdb",attrs:{href:e.imdbUrl,target:"_blank"}},[t._v("IMDB："+t._s(e.imdbScore||"0.0"))]),t._v(" "),i("a",{staticClass:"dou",attrs:{href:e.dbUrl,target:"_blank"}},[t._v("豆瓣："+t._s(e.dbScore||"0.0"))])])])],1)],1)}),t._v(" "),i("div",{staticClass:"ui hidden divider"})],2)},staticRenderFns:[]}},function(t,e){t.exports={render:function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("div",{attrs:{id:"app"}},[i("router-view")],1)},staticRenderFns:[]}},function(t,e){t.exports={render:function(){var t=this,e=t.$createElement;return(t._self._c||e)("div",{class:["vue-pagination-info",t.infoClass],domProps:{innerHTML:t._s(t.paginationInfo)}})},staticRenderFns:[]}},function(t,e){t.exports={render:function(){var t=this,e=t.$createElement,i=t._self._c||e;return i("div",{staticClass:"ui container"},[i("div",{staticClass:"ui hidden divider"}),t._v(" "),t.loading?i("div",{staticClass:"ui active dimmer"},[i("div",{staticClass:"ui loader"})]):t._e(),t._v(" "),i("div",{directives:[{name:"show",rawName:"v-show",value:t.error,expression:"error"}],staticClass:"ui error message",attrs:{transition:"fade"}},[t._v("\n    "+t._s(t.error)+"\n  ")]),t._v(" "),t.movie?i("div",{attrs:{id:"movie"}},[i("h2",[t._v(t._s(t.movie.title))]),t._v(" "),i("div",{staticClass:"ui grid"},[i("div",{staticClass:"five wide column"},[i("img",{attrs:{id:"thumb",src:t.movie.thumb}})]),t._v(" "),i("div",{staticClass:"eleven wide column"},[i("div",{staticClass:"ui items"},[t.isNotEmpty(t.movie._embedded.directors)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("导演")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie._embedded.directors)))])])]):t._e(),t._v(" "),t.isNotEmpty(t.movie._embedded.editors)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("编剧")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie._embedded.editors)))])])]):t._e(),t._v(" "),t.isNotEmpty(t.movie._embedded.actors)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("主演")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie._embedded.actors)))])])]):t._e(),t._v(" "),t.isNotEmpty(t.movie._embedded.categories)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("类型")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie._embedded.categories)))])])]):t._e(),t._v(" "),t.isNotEmpty(t.movie._embedded.regions)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("制片国家/地区")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie._embedded.regions)))])])]):t._e(),t._v(" "),t.isNotEmpty(t.movie._embedded.languages)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("语言")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie._embedded.languages)))])])]):t._e(),t._v(" "),t.movie.releaseDate?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("上映日期")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t.movie.releaseDate))])])]):t._e(),t._v(" "),t.movie.runningTime?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("片长")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t.movie.runningTime))])])]):t._e(),t._v(" "),t.isNotEmpty(t.movie.aliases)?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("又名")]),t._v(" "),i("div",{staticClass:"description"},[t._v(t._s(t._f("join")(t.movie.aliases)))])])]):t._e(),t._v(" "),i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("豆瓣评分")]),t._v(" "),i("div",{staticClass:"description"},[i("a",{attrs:{href:t.movie.dbUrl,target:"_blank"}},[t._v(t._s(t.movie.dbScore||"0.0"))])])])]),t._v(" "),t.movie.imdbUrl?i("div",{staticClass:"item"},[i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[t._v("IMDb链接")]),t._v(" "),i("div",{staticClass:"description"},[i("a",{attrs:{href:t.movie.imdbUrl,target:"_blank"}},[t._v(t._s(t._f("imdb")(t.movie.imdbUrl)))])])])]):t._e()])])]),t._v(" "),i("div",{staticClass:"ui message"},[i("div",{staticClass:"header"},[t._v("剧情简介")]),t._v(" "),i("p",[t._v(t._s(t.movie.synopsis||"暂无介绍"))])]),t._v(" "),t.isNotEmpty(t.movie.res)?[i("div",{staticClass:"ui horizontal divider"},[t._v("资源")]),t._v(" "),i("div",{staticClass:"ui relaxed divided list"},t._l(t.movie.res,function(e){return i("div",{staticClass:"item"},[i("i",{staticClass:"magnet middle aligned icon"}),t._v(" "),i("div",{staticClass:"content"},[i("div",{staticClass:"header"},[i("a",{attrs:{href:e.uri,target:"_blank",title:"点击下载资源"}},[t._v(t._s(e.title||e.uri))]),t._v(" "),e.original?i("a",{attrs:{href:e.original,title:"资源原始地址",target:"_blank"}},[t._v("\n                  "),i("i",{staticClass:"small external icon"})]):t._e()])])])}))]:t._e(),t._v(" "),i("div",{staticClass:"ui hidden divider"})],2):t._e()])},staticRenderFns:[]}},function(t,e){t.exports={render:function(){var t=this,e=t.$createElement,i=t._self._c||e;return t.pagination&&t.pagination.totalPages>0?i("div",{class:t.css.wrapperClass},[i("a",{class:["btn-nav",t.css.linkClass,t.isOnFirstPage?t.css.disabledClass:""],on:{click:function(e){t.loadPage(0)}}},[""!=t.icons.first?i("i",{class:[t.icons.first]}):i("span",[t._v("«")])]),t._v(" "),i("a",{class:["btn-nav",t.css.linkClass,t.isOnFirstPage?t.css.disabledClass:""],on:{click:function(e){t.loadPage("prev")}}},[""!=t.icons.next?i("i",{class:[t.icons.prev]}):i("span",[t._v(" ‹")])]),t._v(" "),t.notEnoughPages?[t._l(t.totalPage,function(e){return[i("a",{class:[t.css.pageClass,t.isCurrentPage(e-1)?t.css.activeClass:""],domProps:{innerHTML:t._s(e)},on:{click:function(i){t.loadPage(e-1)}}})]})]:[t._l(t.windowSize,function(e){return[i("a",{class:[t.css.pageClass,t.isCurrentPage(t.windowStart+e-1)?t.css.activeClass:""],domProps:{innerHTML:t._s(t.windowStart+e)},on:{click:function(i){t.loadPage(t.windowStart+e-1)}}})]})],t._v(" "),i("a",{class:["btn-nav",t.css.linkClass,t.isOnLastPage?t.css.disabledClass:""],on:{click:function(e){t.loadPage("next")}}},[""!=t.icons.next?i("i",{class:[t.icons.next]}):i("span",[t._v("› ")])]),t._v(" "),i("a",{class:["btn-nav",t.css.linkClass,t.isOnLastPage?t.css.disabledClass:""],on:{click:function(e){t.loadPage(t.totalPage-1)}}},[""!=t.icons.last?i("i",{class:[t.icons.last]}):i("span",[t._v("»")])])],2):t._e()},staticRenderFns:[]}},,,,function(t,e){}],[19]);
//# sourceMappingURL=app.38a9096d33429ff84ce9.js.map