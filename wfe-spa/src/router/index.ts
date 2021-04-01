import Vue from 'vue';
import VueRouter from 'vue-router';
import { layout } from './router-helpers';
import Desktop from '../views/Desktop.vue';
import TaskList from '../views/TaskList.vue';
import ProcessDefinitionList from '../views/ProcessDefinitionList.vue';
import ProcessList from '../views/ProcessList.vue';
import TaskCard from '../views/TaskCard.vue';
import ProcessCard from '../views/ProcessCard.vue';
import Profile from '../views/Profile.vue';
import store from '../store';

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    layout('Auth'),
    layout('Default', [
      {
        name: 'Рабочий стол',
        component: Desktop,
        path: '',
      },
      {
        name: 'Мои задачи',
        component: TaskList,
        path: '/task/list/',
      },
      {
        name: 'Запустить процесс',
        component: ProcessDefinitionList,
        path: '/process/definition/list/',
      },
      {
        name: 'Запущенные процессы',
        component: ProcessList,
        path: '/process/list/',
      },
      {
        name: 'Карточка задачи',
        component: TaskCard,
        path: '/task/:id',
      },
      {
        name: 'Карточка процесса',
        component: ProcessCard,
        path: '/process/:id',
      },
      {
        name: 'Профиль',
        component: Profile,
        path: '/profile/'
      }
    ]),
  ],
});

// Здесь глобальный хук, чтобы проверять авторизацию всех маршрутах, кроме /login
router.beforeEach((to, from, next) => {
  if (to.name !== 'Login') {
    store.dispatch('user/authenticate').then(isAuthenticated => {
      next();
    }, notAuthenticated => {
      next({ name: 'Login' });
    });
  } else {
    next();
  }
});

export default router;
