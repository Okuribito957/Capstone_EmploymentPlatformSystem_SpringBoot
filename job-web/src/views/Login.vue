<template>
    <div>
        <div class="login-header">
            <div class="login-main">
                <div class="system-title">
                   轻松投大学生校园招聘
                </div>
            </div>
        </div>
        <div class="login-container">
            <div class="login-main">
                <div class="show"><img src="~@/assets/images/manage-bg.png"></div>
                <div class="box">
                    <div class="title">系统登录</div>
                    <el-form ref="loginForm" :model="form" :rules="rules">
                        <el-form-item prop="account">
                            <el-input v-model="form.account" class="login-user" placeholder="请输入用户名"></el-input>
                        </el-form-item>
                        <el-form-item prop="password">
                            <el-input type="password" v-model="form.password" class="login-pwd"
                                      placeholder="请输入密码"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-select v-model="form.type" placeholder="请选择类型">
                                <el-option
                                        v-for="item in options"
                                        :key="item.value"
                                        :label="item.label"
                                        :value="item.value">
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item>
                            <div class="turnstile" ref="turnstile"></div>
                            <div v-if="turnstileError" class="turnstile-error">{{ turnstileError }}</div>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="login" class="login-btn" :loading="loggingIn">登录</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </div>
        <div class="login-footer">
            本网站信息未经书面许可不得转载，浏览器推荐使用Chrome、FireFox、IE8.0以上版本
        </div>
    </div>
</template>

<script>
    import {message} from "@/utils/message";

    export default {
        name: "Login",
        data() {
            return {
                form: {
                    account: 'admin',
                    password: '123',
                    type: 0,
                },
                loggingIn: false,
                turnstileWidgetId: null,
                turnstileToken: null,
                turnstileError: '',
                options: [{label: '管理员', value: 0}, {label: '企业', value: 1}, {label: '学生', value: 2}],
                rules: {
                    account: [{required: true, message: "用户名不能为空"}],
                    password: [{required: true, message: "密码不能为空"}],
                }
            }
        },
        mounted() {
            this.initTurnstile();
        },
        beforeDestroy() {
            this.removeTurnstile();
        },
        methods: {
            getTurnstileSiteKey() {
                return process.env.VUE_APP_TURNSTILE_SITEKEY;
            },
            waitForTurnstileApi({timeoutMs = 8000, intervalMs = 50} = {}) {
                return new Promise((resolve, reject) => {
                    const startedAt = Date.now();
                    const timer = setInterval(() => {
                        if (window.turnstile && typeof window.turnstile.render === 'function') {
                            clearInterval(timer);
                            resolve(window.turnstile);
                            return;
                        }
                        if (Date.now() - startedAt >= timeoutMs) {
                            clearInterval(timer);
                            reject(new Error('Turnstile 脚本加载超时'));
                        }
                    }, intervalMs);
                });
            },
            async initTurnstile() {
                const sitekey = this.getTurnstileSiteKey();
                if (!sitekey) {
                    this.turnstileError = '未配置 Turnstile sitekey（VUE_APP_TURNSTILE_SITEKEY）';
                    message.warning(this.turnstileError);
                    return;
                }
                if (!this.$refs.turnstile) {
                    return;
                }

                try {
                    this.turnstileError = '';
                    const turnstile = await this.waitForTurnstileApi();
                    this.turnstileToken = null;
                    this.turnstileWidgetId = turnstile.render(this.$refs.turnstile, {
                        sitekey,
                        theme: 'auto',
                        callback: (token) => {
                            this.turnstileToken = token;
                        },
                        'expired-callback': () => {
                            this.turnstileToken = null;
                        },
                        'error-callback': () => {
                            this.turnstileToken = null;
                        }
                    });
                } catch (e) {
                    const detail = e && e.message ? e.message : 'Turnstile 初始化失败';
                    this.turnstileError = detail + '（请检查浏览器能否访问 https://challenges.cloudflare.com ）';
                    // 方便你在云环境直接看浏览器控制台
                    // eslint-disable-next-line no-console
                    console.error('[Turnstile] init failed:', e);
                    message.error(this.turnstileError);
                }
            },
            resetTurnstile() {
                if (window.turnstile && this.turnstileWidgetId !== null) {
                    try {
                        window.turnstile.reset(this.turnstileWidgetId);
                    } catch (e) {
                        // ignore
                    }
                }
                this.turnstileToken = null;
            },
            removeTurnstile() {
                if (window.turnstile && this.turnstileWidgetId !== null) {
                    try {
                        window.turnstile.remove(this.turnstileWidgetId);
                    } catch (e) {
                        // ignore
                    }
                }
                this.turnstileWidgetId = null;
                this.turnstileToken = null;
            },
            login() {
                this.$refs['loginForm'].validate(valid => {
                    if (valid) {
                        if (!this.turnstileToken) {
                            message.warning('请先完成人机验证');
                            return;
                        }
                        this.loggingIn = true;
                        //用户登录操作
                        const payload = {
                            ...this.form,
                            turnstileToken: this.turnstileToken,
                        };
                        this.$store.dispatch('login', payload).then(res => {
                            if (res.code == 200) {
                                this.$router.push('/index');
                            }
                        }).catch(() => {
                            this.resetTurnstile();
                        }).finally(() => {
                            this.loggingIn = false;
                        })
                    }
                })
            }
        }
    }
</script>

<style scoped>
    .login-header {
        height: 20%;
        width: 100%;
        position: absolute;
        top: 0;
    }

    .login-container {
        width: 100%;
        position: absolute;
        top: 15%;
        bottom: 15%;
        background-color: #0F9296;
    }

    .login-main {
        width: 1200px;
        height: 100%;
        margin: auto;
    }

    .system-title {
        font-size: 36px;
        margin-top: 40px;
        margin-left: 60px;
        font-weight: bold;
        color: #555555;
    }

    .show {
        float: left;
        position: absolute;
        bottom: 0;
        left: 150px;
        top: 0;
        padding-top: 50px;
        width: 750px;
    }

    .show img {
        height: 90%;
        display: block;
        margin-left: 20px;
    }

    .box {
        width: 30%;
        padding: 20px 40px;
        float: right;
        background-color: #fff;
        border-radius: 5px;
        margin-top: 8%;
    }

    .box .title {
        font-size: 18px;
        margin-bottom: 20px;
    }

    .login-btn {
        width: 100%;
        background-color: #0F9296;
        border: 0px;
    }

    .login-btn:hover {
        width: 100%;
        background-color: #007B73;
        border: 0px;
    }

    .login-user >>> .el-input__inner {
        background: url('~@/assets/images/user.png') no-repeat center left;
        padding-left: 25px;
    }

    .login-pwd >>> .el-input__inner {
        background: url('~@/assets/images/password.png') no-repeat center left;
        padding-left: 25px;
    }

    .login-footer {
        position: absolute;
        bottom: 0px;
        height: 15%;
        width: 100%;
        text-align: center;
        color: #999999;
        padding-top: 45px;
        font-size: 14px;
        box-sizing: border-box;
        background-color: #F0F0EE;
    }

    .turnstile {
        min-height: 66px;
    }

    .turnstile-error {
        margin-top: 8px;
        color: #F56C6C;
        font-size: 12px;
        line-height: 1.4;
        word-break: break-all;
    }
</style>