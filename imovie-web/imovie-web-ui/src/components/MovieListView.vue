<template>
  <div class="ui container divided items">
    <div v-for="movie in movies" class="item movie" style="min-height: 225px;">
      <router-link :to="'/movies/' + movie.id" class="ui small image">
        <img :src="movie.thumb">
      </router-link>
      <div class="content">
        <router-link :to="'/movies/' + movie.id" class="header">
          {{ movie.title }}
        </router-link>
        <div class="description">
          <p>{{ movie.synopsis || '暂无介绍' }}</p>
        </div>
        <div class="extra">
          <div>
            <span class="date">{{ movie.createdTime | date }}</span>
            <a :href="movie.imdbUrl" target="_blank" class="imdb">IMDB：{{ movie.imdbScore || '0.0' }}</a>
            <a :href="movie.dbUrl" target="_blank" class="dou">豆瓣：{{ movie.dbScore || '0.0' }}</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style>
  div.description {
    min-height: 150px;
    text-align: left;
  }
  span.date {
    color: #8f8f8f;
    position: absolute;
    left: 5px;
  }
  a.imdb {
    color: #f2992e;
    position: absolute;
    right: 75px;
  }
  a.dou {
    color: #56bc8a;
    position: absolute;
    right: 0px;
  }

</style>
<script>
import movieService from '@/services/MovieService'

export default {
  name: 'MovieListView',
  data () {
    return {
      loading: false,
      error: '',
      page: this.$route.query.page || 0,
      pagination: null,
      movies: [
        {
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2398194347.jpg',
          'dbUrl': 'https://movie.douban.com/subject/26892555/',
          'dbScore': '8.3',
          'imdbUrl': 'http://www.imdb.com/title/tt6234398',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:20.000+0000',
          'title': '哦，我的金雨 오 마이 금비 (2016)',
          'synopsis': '　　讲述一个爸爸在照顾患有小儿痴呆症，记忆丧失的8岁女儿的过程中领悟到人生价值的故事。',
          'id': 21
        },
        {
          'synopsis': '　　《少女特工队》是一部将一系列特工间谍片打乱后，再重新包装的青春动作片。 　　每一年高中生都需要经过SAT的考试，而政府通过这项测试暗中考验她们的特别天分。取得高分者会被征收进入一所秘密学园——D.E.B.S.。经过严格的培训后，四个集美貌和智慧于一身的女孩组成一支特工队。艾米（莎拉·福斯特 Sara Foster 饰）每项技能都取得高分，学院全优毕业；麦克丝（梅根·古德 Meagan Good 饰）暴脾气，但雷厉风行的作风深得队友爱戴；珍妮特（吉尔·里奇 Jill Ritchie 饰）容易害羞，说起谎来却有一套；多米妮克（戴文青木 Devon Aoki 饰）喜欢抽烟，总不缺男伴。这一次她们的任务是对付头号珠宝女贼露茜（乔丹娜·布鲁斯特 Jordana Brewster 饰）。艾米发现她并非传闻那么可怕，更在机缘巧合下爱上了这个女贼......',
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2250194439.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1309039/',
          'dbScore': '6.0',
          'imdbUrl': 'http://www.imdb.com/title/tt0367631',
          'imdbScore': null,
          'createdTime': '2017-05-29T02:49:20.000+0000',
          'title': '少女特工队 D.E.B.S. (2004)',
          'id': 14166
        },
        {
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2459723975.webp',
          'dbUrl': 'https://movie.douban.com/subject/6311303/',
          'dbScore': '7.1',
          'imdbUrl': 'http://www.imdb.com/title/tt1202194',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:03:54.000+0000',
          'title': '加勒比海盗5：死无对证 Pirates of the Caribbean: Dead Men Tell No Tales (2017)',
          'synopsis': '故事发生在《加勒比海盗3：世界的尽头》沉船湾之战20年后。男孩亨利（布兰顿·思怀兹 Brenton Thwaites 饰）随英国海军出航寻找被聚魂棺诅咒的父亲“深海阎王”威尔·特纳（奥兰多·布鲁姆 Orlando Bloom 饰），却在百慕大三角遭遇被解封的亡灵萨拉查船长（哈维尔·巴登 Javier Bardem 饰）。获取自由的萨拉查屠尽加勒比海盗，征服了整个海域。里海海盗王赫克托·巴博萨船长（杰弗里·拉什 Geoffrey Rush 饰）在女巫Haifaa Meni（格什菲·法拉哈尼 Golshifteh Farahani 饰）口中得知了萨拉查的真实目的：为寻找他的宿敌杰克船长（约翰尼·德普 Johnny Depp 饰）。海盗的命运皆压在落魄的老杰克被封印的黑珍珠号，以及天文学家卡琳娜·史密斯（卡雅·斯考达里奥 Kaya Scodelario 饰... ',
          'id': 4
        },
        {
          'title': '哦，我的金雨 오 마이 금비 (2016)',
          'synopsis': '　　讲述一个爸爸在照顾患有小儿痴呆症，记忆丧失的8岁女儿的过程中领悟到人生价值的故事。',
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2398194347.jpg',
          'dbUrl': 'https://movie.douban.com/subject/26892555/',
          'dbScore': '8.3',
          'imdbUrl': 'http://www.imdb.com/title/tt6234398',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:20.000+0000',
          'id': 21
        }, {
          'title': '凄艳断肠花 The Paradine Case (1947)',
          'synopsis': '　　帕拉丁夫人（阿莉达·瓦莉 Alida Valli 饰）被控告杀害了自己的丈夫，她找来了名叫安东尼（格利高里·派克 Gregory Peck 饰）的知名律师为自己辩护。随着交往的深入，安东尼发现自己竟然渐渐地爱上了美艳的帕拉丁夫人，然而安东尼已经娶了单纯善良的妻子盖伊（安·托德 Ann Todd 饰），每每想到妻子的脸庞，安东尼的心里就感到格外内疚。 　　然而，对于帕拉丁夫人的迷恋战胜了安东尼的内疚感，一次偶然中，安东尼发现帕拉丁夫人身旁的仆人安德鲁（路易斯·乔丹 Louis Jourdan 饰）最有嫌疑，尽管并没有安德鲁就是凶手的确凿证据，但嫁祸于安德鲁的确是能够让帕拉丁夫人脱罪的最好方式。',
          'thumb': 'https://img3.doubanio.com/lpic/s2576486.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1294542/',
          'dbScore': '6.7',
          'imdbUrl': 'http://www.imdb.com/title/tt0039694',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:20.000+0000',
          'id': 22
        }, {
          'title': '哦，我的爸爸！！ Oh, My Dad!! (2013)',
          'synopsis': '　　四处奔走依靠科普讲座勉强赚取稀薄生活费的新海元一（织田裕二 饰），许多年前曾是科学界一颗冉冉升起的明日之星，他致力于研究镁燃料电池，但是实验和项目接连遭受挫折。日常操劳家中一切的妻子纱世子（铃木幸树 饰）忍无可忍，离家出走。此时元一才意识到，自己的小家已经到了濒临崩溃的地步。因拖欠房租，他和儿子光太（田中奏生 饰）被赶出公寓；曾经支持他的业者也家财散尽，灰心离去。走投无路的元一各方求职，奔走在单位和幼儿园两端，偶尔求助于旧知岸田（八岛智人 饰）和昔日恋人美月（长谷川京子 饰）。疲于奔命，与生活搏斗。 　　他是儿子心中最伟大甚至能发明任意门的科学家，他又是被残酷现实压得几近窒息却仍不放弃一线希望的平凡男人……',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2145635914.jpg',
          'dbUrl': 'https://movie.douban.com/subject/24532102/',
          'dbScore': '6.1',
          'imdbUrl': 'http://www.imdb.com/title/tt2946966',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:22.000+0000',
          'id': 23
        }, {
          'title': '我的情人 (1971)',
          'synopsis': null,
          'thumb': 'https://img3.doubanio.com/lpic/s11148063.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1301945/',
          'dbScore': '5.1',
          'imdbUrl': 'http://www.imdb.com/title/tt0067519',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:24.000+0000',
          'id': 24
        }, {
          'title': '征服钱海 The Big Kahuna (1999)',
          'synopsis': '　　赖瑞是个愤世嫉俗的推销员。而才和老婆离婚的菲尔，高业绩已不再让他兴奋。现在又加入菜鸟鲍伯。三个作风迥异的业务员这次必须联手抢下生意大鱼。在绞尽脑汁追逐金钱之际，阻碍他们成功的，竟然是个「人性」问题?!',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p1127809052.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1306586/',
          'dbScore': '7.3',
          'imdbUrl': 'http://www.imdb.com/title/tt0189584',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:28.000+0000',
          'id': 25
        }, {
          'title': '战争机器 War Machine (2017)',
          'synopsis': '　　《战争机器》改编自已故记者迈克尔·哈斯汀斯的畅销书《操纵者：阿富汗战争的可怕内幕》，影片将以黑色幽默的形式聚焦美国军工联合体。原著中的核心人物为以美国前任驻阿富汗美军最高指挥官斯坦利·麦克里斯特尔为原型的四星上将。据悉，布拉德·皮特将饰演该角色，他策划并发动了阿富汗战争。为了以全新方法赢得这场“不可能”的战争，将军与他的下属们可谓煞费苦心。他们一方面要主导国际联盟，掌控军事需求以及华盛顿的军事政策，还要满足媒体的报道需求，更要花费精力“管理战争”——要时时刻刻与身在战场中的人保持联系。',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2458748574.jpg',
          'dbUrl': 'https://movie.douban.com/subject/11518990/',
          'dbScore': '6.3',
          'imdbUrl': 'http://www.imdb.com/title/tt4758646',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:32.000+0000',
          'id': 26
        }, {
          'title': '克罗索巨兽 Colossal (2016)',
          'synopsis': '　　影片讲述一个在纽约丢了男友(史蒂文斯饰)失了工作的女人(海瑟薇饰)，回到老家后，无意间发现自己和某种怪兽有着诡异的联系。',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2437679580.jpg',
          'dbUrl': 'https://movie.douban.com/subject/26388164/',
          'dbScore': '5.8',
          'imdbUrl': 'http://www.imdb.com/title/tt4680182',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:35.000+0000',
          'id': 27
        }, {
          'title': '赶尽杀绝 Shoot \'Em Up (2007)',
          'synopsis': '　　漆黑的夜里，史密斯（克里夫•欧文 Clive Owen 饰）无意中被卷入了一场黑帮的追杀，穿越激烈的枪林弹雨，他解救了一个在襁褓中的婴儿。婴儿的啼哭，让这个功夫了得的铁汉遭遇了难言的尴尬。除了在超市购置婴儿用品之外，他还得解决婴儿的喂奶问题。这时，他想到了在妓院工作的朋友DQ（莫妮卡•贝鲁奇 Monica Bellucci 饰），并强行赶走了嫖客，把婴儿托付给她。DQ其实对史密斯怀有深情，却一直没有机会表白。在蜂拥而至的黑帮暴徒来临之际，两人终于坦诚相见，并肩作战，照顾婴儿。经过史密斯的调查，他们发现想要加害婴儿的黑帮其实跟军火商有关系，而且他们还雇佣了很多代孕妈妈。史密斯隐隐觉得这背后隐藏着一个不可告人的秘密，于是他决定铤而走险，一场正邪较量由此展开……',
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p1708240488.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1947080/',
          'dbScore': '7.4',
          'imdbUrl': 'http://www.imdb.com/title/tt0465602',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:39.000+0000',
          'id': 28
        }, {
          'title': '赶尽杀绝 Kill\'em All (2017)',
          'synopsis': '　　After a massive shootout, a mysterious stranger (Van Damme) arrives at a local hospital on the brink of death. Then, a foreign gang brazenly comes to the hospital to hunt him down. His ...@艾米电影网',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2457037485.jpg',
          'dbUrl': 'https://movie.douban.com/subject/26881142/',
          'dbScore': '',
          'imdbUrl': 'http://www.imdb.com/title/tt5767628',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:40.000+0000',
          'id': 29
        }, {
          'title': '欢乐树的朋友们：赶尽杀绝 Happy Tree Friends: Overkill (2005)',
          'synopsis': null,
          'thumb': 'https://img3.doubanio.com/lpic/s1806732.jpg',
          'dbUrl': 'https://movie.douban.com/subject/3075070/',
          'dbScore': '9.2',
          'imdbUrl': 'http://www.imdb.com/title/tt1135762',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:42.000+0000',
          'id': 30
        }, {
          'title': 'On the Double (1961)',
          'synopsis': null,
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2164578199.jpg',
          'dbUrl': 'https://movie.douban.com/subject/5059416/',
          'dbScore': '',
          'imdbUrl': 'http://www.imdb.com/title/tt0055253',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:32.000+0000',
          'id': 31
        }, {
          'title': '情有千千劫 (2002)',
          'synopsis': '　　迟鸣（李幼斌 饰）是一名刑警，在工作上，他经验丰富尽责职守，是值得信赖的好搭档，在生活中，他充满情趣懂得享受，是天衣无缝的好伴侣。迟鸣曾经和名叫徐若风（于小慧 饰）的女子有过一段恋情，可惜有缘无分的两人最终因为一场误会而分道扬镳，尽管早已经不在一起，但迟鸣的心中从来就没有忘记过徐若风。 　　让迟鸣没有想到的是，本以为再也不会重逢的徐若风，竟然成为了自己的顶头上司，当感情和工作纠葛到一起时，迟鸣该如何处理自己内心的激荡？而徐若风的心中，是否和迟鸣一样，还有这一丝遗憾和留恋呢？犯罪分子的行动从未终结，迟鸣和徐若风合作，破获了一起有一起案件，两人之间的距离也因此越来越近。',
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2375038857.jpg',
          'dbUrl': 'https://movie.douban.com/subject/3558794/',
          'dbScore': '8.5',
          'imdbUrl': null,
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:43.000+0000',
          'id': 32
        }, {
          'title': '警匪生死劫 Ricochet (1991)',
          'synopsis': null,
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2220720661.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1293188/',
          'dbScore': '6.3',
          'imdbUrl': 'http://www.imdb.com/title/tt0102789',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:45.000+0000',
          'id': 33
        }, {
          'title': '赶尽杀绝 趕盡殺絕 (2000)',
          'synopsis': '　　七十年代，南洋曾发生一桩骇人听闻的杀童案件，主凶正是当地一邪教教主次先生。次原是医院里的小杂役，但他性格孤僻，思想偏激，对宗教更有着一份狂热崇拜。机缘巧合下，冼拜了自认法力高强的师父为师，原来这位师父是“神棍”一名。凭他的智能，很快便代了师父的地位，声名在坊间开始流传，俗众亦渐多。他无需强求，金钱、女人，源源的送到他的面前，他开始目空一切，以教主身份自居。一个普通的凡人，竟然被盲目的群众捧成神一样，最可布的就是连自己亦沉迷自己编造的谎言中，自以为是神！终于闯出了无可救的大祸，他要以小童的生命来祭神，两条小生命因此被夺去。天网恢恢，次最终也受到法律的惩罚，判问吊死刑，可惜，在他致死的一刻，他鬼迷心窍的仍不醒，他仍坚持自己是神',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2213142526.jpg',
          'dbUrl': 'https://movie.douban.com/subject/26269535/',
          'dbScore': '',
          'imdbUrl': null,
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:46.000+0000',
          'id': 34
        }, {
          'title': '趕盡殺絕 (1973)',
          'synopsis': null,
          'thumb': 'https://img1.doubanio.com/lpic/s11096688.jpg',
          'dbUrl': 'https://movie.douban.com/subject/10767173/',
          'dbScore': '',
          'imdbUrl': 'http://www.imdb.com/title/tt0187048',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:48.000+0000',
          'id': 35
        }, {
          'title': '赶尽杀绝 (2016)',
          'synopsis': '　　老鬼，中国退伍特种部队成员，在外海执行雇佣赏金任务，由于情报出差错，定点清除目标任务时候，发现目标妻子与孩子也在现场，由于时间紧迫，没有时间做出太多选择，只能清理现场，目标被击中要害，但没有死，目标在自己失血过多昏迷前，看到老鬼把自己的妻儿拉离现场，目标失去意识的同时，听到枪声。老鬼带着目标妻子，在内地开了酒吧，安静的生活着。 多年后，老鬼在家乡无意中卷入了与当地富二代的冲突之中，为了一个小女孩，和富二代家族结下梁子。 富二代无休止的纠缠让老鬼痛下决心，清理对方以消除对自己及情人的伤害。同时，老鬼知道了自己救的小女孩竟然是自己多年前离弃的女儿，富二代为了报复老鬼绑架了小女孩。与此同时，老鬼当年没杀死的目标也浮出水面。是阴谋还是巧合？一场明与暗的较量与厮杀逐渐展开。。',
          'thumb': 'https://img3.doubanio.com/f/movie/30c6263b6db26d055cbbe73fe653e29014142ea3/pics/movie/movie_default_large.png',
          'dbUrl': 'https://movie.douban.com/subject/26864455/',
          'dbScore': '',
          'imdbUrl': null,
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:49.000+0000',
          'id': 36
        }, {
          'title': '杀妻记 How to Murder Your Wife (1965)',
          'synopsis': '　　对婚姻怀有恐惧感的漫画家杰克．李蒙，在一场单身狂欢舞会中因为酒醉而糊里糊涂地娶了从蛋糕中跳出来娱乐宾客的金发美女维尔娜．丽丝。当他清醒过来之后，便想尽办法要摆脱这个妻子时，维尔娜．丽丝真的不见了，所有人都认为他就是凶手。',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2234543471.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1465467/',
          'dbScore': '7.0',
          'imdbUrl': 'http://www.imdb.com/title/tt0058212',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:47.000+0000',
          'id': 37
        }, {
          'title': '吉人天相 Knock on Wood (1954)',
          'synopsis': null,
          'thumb': 'https://img3.doubanio.com/lpic/s3865122.jpg',
          'dbUrl': 'https://movie.douban.com/subject/1299758/',
          'dbScore': '',
          'imdbUrl': 'http://www.imdb.com/title/tt0047152',
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:52.000+0000',
          'id': 38
        }, {
          'title': '怨灵宿舍之人偶老师 (2017)',
          'synopsis': '　　巴洛克画院学生小茶神秘失踪，同宿舍的雨烟等众人在顾老师的帮助下开始追查小茶的下落。在寻找过程中他们意外得知埋藏在校园内十年之久的“人偶老师”神秘传说。玉子，一个已经意外死去十年的画院老师再度回到众人的视野中。随着调查思路的逐渐清晰，玉子老师的死因与“人偶老师”传说背后的真相慢慢浮出水面，而昔日平静的校园却变得危机四伏，寻找小茶下落的同伴接连离奇消失，危难时刻雨烟独自一人走到了探寻所有离奇事件真相的凄惨绝境……',
          'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2450440807.jpg',
          'dbUrl': 'https://movie.douban.com/subject/27003615/',
          'dbScore': '',
          'imdbUrl': null,
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:53.000+0000',
          'id': 39
        }, {
          'title': '怨灵宿舍之白纸女生 (2017)',
          'synopsis': '　　讲述巴洛克画院的学生雨烟（谢容儿 饰）偶然梦见自己已经离世的同学苏苏，而后陷入灵异事件之中，神秘的纸人如梦靥一般缠住雨烟，苏苏借纸人还魂复仇的传说不胫而走，雨烟遭遇的种种异象表明，是死去的苏苏在还魂复仇。深夜隐没的无面鬼，暗夜游荡的白衣邪灵，预示着死亡的浓烈气息，一个尘封多年的秘密被揭晓，而雨烟却陷入了死亡之地。',
          'thumb': 'https://img3.doubanio.com/view/movie_poster_cover/lpst/public/p2452283136.jpg',
          'dbUrl': 'https://movie.douban.com/subject/26948814/',
          'dbScore': '2.4',
          'imdbUrl': null,
          'imdbScore': null,
          'createdTime': '2017-05-28T03:04:55.000+0000',
          'id': 40
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      // this.error = this.movies = null
      this.loading = true
      movieService.getAll(this.page, (success, data) => {
        this.loading = false
        if (success) {
          this.movies = data._embedded.movies
          this.pagination = data.page
        } else {
          this.error = data.message || 'Bad Request'
        }
      })
    }
  }
}

</script>
