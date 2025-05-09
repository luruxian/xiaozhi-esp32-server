pipeline {
    agent any
    
    stages {
        stage('部署manager-api') {
            steps {
                echo '部署manager-api服务 (端口8002)'
                sh '''
                    # 停止旧服务
                    if sudo systemctl is-active --quiet xiaozhi-api; then
                        echo "停止现有 xiaozhi-api 服务..."
                        sudo systemctl stop xiaozhi-api
                    fi

                    # 构建项目
                    cd main/manager-api
                    mvn clean install -DskipTests

                    # 启动服务
                    sudo systemctl start xiaozhi-api

                    # 等待服务变为 active，最多尝试10次，每次间隔3秒
                    for i in {1..20}; do
                        if sudo systemctl is-active --quiet xiaozhi-api; then
                            echo "xiaozhi-api 启动成功！"
                            break
                        else
                            echo "等待 xiaozhi-api 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    # 最终验证状态
                    if ! sudo systemctl is-active --quiet xiaozhi-api; then
                        echo "ERROR: manager-api 启动失败！"
                        exit 1
                    fi
                '''
            }
        }

        stage('部署manager-web') {
            steps {
                echo '部署manager-web服务 (端口8001)'
                sh '''
                    # 停止旧服务
                    if sudo systemctl is-active --quiet xiaozhi-web; then
                        echo "停止现有 xiaozhi-web 服务..."
                        sudo systemctl stop xiaozhi-web
                    fi

                    # 启动服务
                    sudo systemctl start xiaozhi-web

                    # 等待服务变为 active，最多尝试10次，每次间隔3秒
                    for i in {1..20}; do
                        if sudo systemctl is-active --quiet xiaozhi-web; then
                            echo "xiaozhi-web 启动成功！"
                            break
                        else
                            echo "等待 xiaozhi-web 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    # 最终验证状态
                    if ! sudo systemctl is-active --quiet xiaozhi-web; then
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
                    if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
                        echo "停止现有 xiaozhi-server 服务..."
                        pkill -f "python -u app.py" || true
                        sleep 5
                    fi

                    # 启动服务
                    cd main/xiaozhi-server
                    source /home/ubuntu/anaconda3/etc/profile.d/conda.sh
                    conda activate xiaozhi-esp32-server
                    nohup python -u app.py > ./tmp/server.log 2>&1 &

                    # 等待端口监听，最多尝试10次
                    for i in {1..20}; do
                        if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
                            echo "xiaozhi-server 启动成功！"
                            break
                        else
                            echo "等待 xiaozhi-server 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    # 最终验证
                    if ! lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
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
