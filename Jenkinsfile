pipeline {
    agent any

    stages {
        stage('部署manager-api') {
            steps {
                echo '部署manager-api服务 (端口8002)'
                sh '''#!/bin/bash
                    # 释放8002端口
                    if lsof -i:8002 -sTCP:LISTEN -t >/dev/null ; then
                        echo "检测到8002端口已有服务，正在关闭..."
                        kill -9 $(lsof -i:8002 -sTCP:LISTEN -t)
                        sleep 3
                    fi

                    # 构建并部署manager-api
                    cd main/manager-api
                    mvn clean install
                    cd target
                    nohup java -jar xiaozhi-esp32-api.jar --spring.profiles.active=dev >/dev/null 2>&1 &

                    # 等待端口监听
                    for i in {1..20}; do
                        if lsof -i:8002 -sTCP:LISTEN -t >/dev/null ; then
                            echo "manager-api 启动成功！"
                            break
                        else
                            echo "等待 manager-api 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    if ! lsof -i:8002 -sTCP:LISTEN -t >/dev/null ; then
                        echo "ERROR: manager-api 启动失败！"
                        exit 1
                    fi
                '''
            }
        }

        stage('部署manager-web') {
            steps {
                echo '部署manager-web服务 (端口8001)'
                sh '''#!/bin/bash
                    # 释放8001端口
                    if lsof -i:8001 -sTCP:LISTEN -t >/dev/null ; then
                        echo "检测到8001端口已有服务，正在关闭..."
                        kill -9 $(lsof -i:8001 -sTCP:LISTEN -t)
                        sleep 3
                    fi

                    # 构建并部署manager-web
                    cd main/manager-web
                    npm install
                    nohup npm run serve > server.log 2>&1 &

                    # 等待端口监听
                    for i in {1..20}; do
                        if lsof -i:8001 -sTCP:LISTEN -t >/dev/null ; then
                            echo "manager-web 启动成功！"
                            break
                        else
                            echo "等待 manager-web 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    if ! lsof -i:8001 -sTCP:LISTEN -t >/dev/null ; then
                        echo "ERROR: manager-web 启动失败！"
                        exit 1
                    fi
                '''
            }
        }

        stage('部署xiaozhi-server') {
            steps {
                echo '部署xiaozhi-server服务 (端口8000)'
                sh '''#!/bin/bash
                    # 检查并释放端口8000
                    if lsof -i:8000 -sTCP:LISTEN -t >/dev/null ; then
                        echo "停止现有 xiaozhi-server 服务..."
                        pkill -f "python -u app.py" || true
                        sleep 5
                    fi

                    # 启动服务
                    cd main/xiaozhi-server
                    source /home/ubuntu/anaconda3/etc/profile.d/conda.sh
                    conda activate xiaozhi-esp32-server
                    nohup python -u app.py > ./tmp/server.log 2>&1 &

                    # 等待端口监听
                    for i in {1..20}; do
                        if lsof -i:8000 -sTCP:LISTEN -t >/dev/null ; then
                            echo "xiaozhi-server 启动成功！"
                            break
                        else
                            echo "等待 xiaozhi-server 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    if ! lsof -i:8000 -sTCP:LISTEN -t >/dev/null ; then
                        echo "ERROR: xiaozhi-server 启动失败！"
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo '所有服务部署成功！'
        }
        failure {
            echo '部署过程中出现错误！'
        }
    }
}
